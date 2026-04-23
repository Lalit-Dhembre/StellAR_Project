from flask import Blueprint, request, jsonify, current_app
import os
import uuid
import json
import logging
from typing import Dict, Any, List

from modules.api.ocr import extract_text_from_file
from modules.rag.text_chunker import chunk_text
from modules.rag.concept_extractor import extract_concepts
from modules.rag.image_retriever import retrieve_images
from modules.rag.audio_generator import generate_script, generate_scripts_for_chunk
from modules.rag.model_resolver import resolve_3d_model
from modules.api.domain_validator import validate_domain
from modules.rag.pdf_image_extractor import extract_and_upload_pdf_images, upload_raw_image

logger = logging.getLogger(__name__)

rag_orchestrator_bp = Blueprint('rag_orchestrator', __name__, url_prefix='/api/rag')

# -------------------------------
# Redis Cache Configuration
# -------------------------------
# We initialize Redis lazily and remember failures to avoid repeated timeouts.
_redis_client = None
_redis_disabled = False

def get_redis():
    global _redis_client, _redis_disabled
    if _redis_disabled:
        return None
    if _redis_client is not None:
        return _redis_client
    try:
        import redis
        redis_url = os.environ.get("REDIS_URL") or os.environ.get("CELERY_BROKER_URL") or "redis://localhost:6379/0"
        client = redis.Redis.from_url(redis_url, decode_responses=True, socket_connect_timeout=1)
        client.ping()
        _redis_client = client
        return _redis_client
    except Exception as e:
        logger.warning(f"Redis unavailable, using in-memory cache for concepts: {e}")
        _redis_disabled = True
        return None

_fallback_cache = {}

_INTERNAL_CONCEPT_FIELDS = {
    "source_chunk",
    "source_chunk_index",
}

def cache_concept(concept_id: str, data: dict):
    """Store a concept in Redis, fallback to local dict."""
    _fallback_cache[concept_id] = data  # Always store locally
    r = get_redis()
    if r:
        try:
            r.setex(f"stellar:concept:{concept_id}", 86400, json.dumps(data))
        except Exception:
            pass

def get_cached_concept(concept_id: str) -> Dict[str, Any]:
    """Retrieve a concept from Redis, fallback to local dict."""
    r = get_redis()
    if r:
        try:
            data = r.get(f"stellar:concept:{concept_id}")
            if data:
                return json.loads(data)
        except Exception:
            pass
    return _fallback_cache.get(concept_id)


def _public_concept_payload(concept: Dict[str, Any]) -> Dict[str, Any]:
    """Strip internal pipeline-only fields before caching or returning to the app."""
    return {
        key: value
        for key, value in concept.items()
        if key not in _INTERNAL_CONCEPT_FIELDS
    }

# -------------------------------
# Endpoints
# -------------------------------

@rag_orchestrator_bp.route('/process-content', methods=['POST'])
def process_content():
    """
    Input: file (PDF/image/text).
    Steps:
      1. Extract text
      2. Chunk text
      3. Extract concepts
      4. Retrieve images
    Output: { "concepts": [ ... ] }
    """
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'}), 400

    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400

    # Ensure output directory exists
    output_dir = current_app.config.get('OUTPUT_DIR', 'temp_uploads')
    os.makedirs(output_dir, exist_ok=True)

    temp_id = str(uuid.uuid4())
    ext = os.path.splitext(file.filename)[1].lower()
    temp_path = os.path.join(output_dir, f"temp_process_{temp_id}{ext}")
    file.save(temp_path)

    try:
        # Step 1: Extract Text
        logger.info(f"Extracting text from {file.filename}")
        extracted_text = extract_text_from_file(temp_path)
        
        if not extracted_text or not extracted_text.strip():
            return jsonify({'error': 'Could not extract text from the file.'}), 400

        # Step 1.5: Domain Validation
        expected_domain = request.form.get("expected_domain", "biology")
        logger.info(f"Validating domain: expected '{expected_domain}'")
        validation = validate_domain(extracted_text, expected_domain)
        if not validation.get("match"):
            return jsonify({
                "error": "Document domain mismatch.",
                "validation": validation
            }), 400

        # Step 1.8: Extract Native Images (Dual Path)
        native_images = []
        if ext == '.pdf':
            logger.info("Extracting native embedded images from PDF")
            native_images = extract_and_upload_pdf_images(temp_path, bucket_name="images")
        elif ext in ['.jpg', '.jpeg', '.png', '.webp']:
            logger.info("Processing direct image upload (Camera).")
            native_images = upload_raw_image(temp_path, bucket_name="images")

        # Step 2: Semantic Chunking
        logger.info("Chunking text")
        chunks = chunk_text(extracted_text)

        # Step 3: Extract Concepts (LLM)
        logger.info(f"Extracting concepts from {len(chunks)} chunks")
        all_concepts = extract_concepts(chunks, domain=expected_domain)
        
        # extract_concepts already returns a flat, deduplicated list.
        # Ensure every concept has an 'id' and cap at 30.
        flat_concepts = []
        for concept in all_concepts:
            if not isinstance(concept, dict):
                continue
            if "id" not in concept:
                concept["id"] = f"concept-{uuid.uuid4().hex[:8]}"
            flat_concepts.append(concept)
            if len(flat_concepts) >= 30:
                break

        # Step 4: Retrieve Images + Generate Scripts chunk-by-chunk
        # This keeps narration grounded in the source chunk and ensures scripts
        # are ready before the app selects a concept.
        logger.info(f"Retrieving images and scripts for {len(flat_concepts)} concepts")
        concepts_by_chunk: Dict[int, List[Dict[str, Any]]] = {}
        for concept in flat_concepts:
            raw_chunk_index = concept.get("source_chunk_index", -1)
            try:
                chunk_index = int(raw_chunk_index)
            except (TypeError, ValueError):
                chunk_index = -1
            concepts_by_chunk.setdefault(chunk_index, []).append(concept)

        enriched_concepts = []
        for chunk_index, chunk_concepts in concepts_by_chunk.items():
            logger.info(
                "Processing chunk %s with %d concept(s)",
                chunk_index,
                len(chunk_concepts),
            )

            chunk_with_images = []
            for concept in chunk_concepts:
                title = concept.get("title", "?")
                try:
                    enriched = retrieve_images(concept)
                except Exception as img_err:
                    logger.error(f"Image retrieval failed for '{title}': {img_err}")
                    enriched = dict(concept)
                    enriched.setdefault("image_url", None)
                    enriched.setdefault("image_caption", None)
                    enriched.setdefault("source", "error")
                chunk_with_images.append(enriched)

            chunk_text = next(
                (concept.get("source_chunk", "") for concept in chunk_with_images if concept.get("source_chunk")),
                "",
            )
            script_map = generate_scripts_for_chunk(chunk_with_images, chunk_text=chunk_text)

            for concept in chunk_with_images:
                concept_id = concept.get("id")
                script = script_map.get(concept_id)
                if not script:
                    logger.warning(
                        "Falling back to single-concept script generation for '%s'",
                        concept.get("title", "?"),
                    )
                    script = generate_script(concept)
                concept["script"] = script

                public_concept = _public_concept_payload(concept)
                if concept_id:
                    cache_concept(concept_id, public_concept)
                enriched_concepts.append(public_concept)

        return jsonify({
            "success": True,
            "concepts": enriched_concepts,
            "native_images": native_images
        }), 200

    except Exception as e:
        logger.error(f"Error in process-content: {e}")
        return jsonify({"error": str(e)}), 500

    finally:
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except OSError:
                pass


@rag_orchestrator_bp.route('/concept-details', methods=['POST'])
def concept_details():
    """
    Input: { "concept_id": str }
    Steps:
      1. Fetch cache
      2. Generate script
      3. Async 3D generation
    Output: { "title", "image_url", "script", "model_url", "model_status" }
    """
    data = request.json or {}
    concept_id = data.get("concept_id")

    if not concept_id:
        return jsonify({"error": "Missing concept_id parameter"}), 400

    # Step 1: Fetch concept from memory/cache
    concept = get_cached_concept(concept_id)
    if not concept:
        return jsonify({"error": f"Concept '{concept_id}' not found or expired"}), 404

    try:
        # Step 2: Generate or retrieve script
        if "script" not in concept:
            logger.info(f"Generating script for concept: {concept.get('title')}")
            script = generate_script(concept)
            concept["script"] = script
            
            # Save the generated script back to cache
            cache_concept(concept_id, concept)

        # Step 3: Resolve 3D Model
        logger.info(f"Resolving 3D model for concept: {concept.get('title')}")
        # The resolver checks local/supabase and enqueues async generation
        model_res = resolve_3d_model(concept)
        
        # Update concept with latest model status (optional, but good for caching state)
        concept["model_url"] = model_res.get("model_url")
        concept["model_status"] = model_res.get("status")
        cache_concept(concept_id, concept)

        return jsonify({
            "title": concept.get("title"),
            "image_url": concept.get("image_url"),
            "script": concept.get("script"),
            "model_url": model_res.get("model_url"),
            "model_status": model_res.get("status")
        }), 200

    except Exception as e:
        logger.error(f"Error in concept-details: {e}")
        return jsonify({"error": str(e)}), 500
