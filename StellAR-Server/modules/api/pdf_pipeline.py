"""
PDF Ingestion Pipeline — All 5 Phases

Phase 1: Structured PDF Ingestion (text + image extraction, structural parsing)
Phase 2: Semantic Validation Layer (Grok domain classification + confidence gate)
Phase 3: Concept Chunking (heading-boundary-aware grouping)
Phase 4: Hybrid Asset Decision Engine (DB lookup + generative fallback)
Phase 5: Asynchronous Orchestration (parallel quiz, tutor script, TTS generation)
"""

import os
import uuid
import json
import logging
import tempfile
import threading
from pathlib import Path
from typing import Dict, Any, List, Optional, Tuple

from flask import Blueprint, request, jsonify, current_app, send_file

logger = logging.getLogger(__name__)

pdf_bp = Blueprint('pdf', __name__, url_prefix='/api/pdf')

# In-memory job store for async orchestration (Phase 5)
# In production, use Redis or a database
_jobs: Dict[str, Dict[str, Any]] = {}

# ------------------------------------------------------------------
# Phase 1 — Structured PDF Ingestion
# ------------------------------------------------------------------

def extract_pdf_content(pdf_path: str) -> Dict[str, Any]:
    """
    Extract text, embedded images, and structural elements from a PDF.
    Uses PyMuPDF (fitz) for fast, layout-preserving extraction.
    
    Returns:
        {
            "title": str or None,
            "page_count": int,
            "sections": [ {type, level, text, page, image_id?, caption?} ],
            "figures": [ {id, caption, local_path} ],
            "raw_text": str
        }
    """
    import fitz  # PyMuPDF

    doc = fitz.open(pdf_path)
    sections = []
    figures = []
    raw_text_parts = []
    figure_counter = 0

    # Create temp dir for extracted images
    figures_dir = os.path.join(tempfile.gettempdir(), "stellar_pdf_figures", str(uuid.uuid4()))
    os.makedirs(figures_dir, exist_ok=True)

    inferred_title = None

    for page_num in range(len(doc)):
        page = doc[page_num]
        page_number = page_num + 1

        # ---- Extract text blocks with font info for structural parsing ----
        blocks = page.get_text("dict", flags=fitz.TEXT_PRESERVE_WHITESPACE)["blocks"]

        for block in blocks:
            # Image block
            if block.get("type") == 1:
                figure_counter += 1
                fig_id = f"fig_{figure_counter:03d}"
                
                # Save embedded image
                try:
                    img_ext = block.get("ext", "png")
                    img_path = os.path.join(figures_dir, f"{fig_id}.{img_ext}")
                    with open(img_path, "wb") as img_file:
                        img_file.write(block["image"])
                    
                    figures.append({
                        "id": fig_id,
                        "caption": "",  # Captions inferred below
                        "local_path": img_path,
                        "page": page_number,
                    })
                    sections.append({
                        "type": "figure",
                        "text": "",
                        "image_id": fig_id,
                        "caption": "",
                        "page": page_number,
                    })
                except Exception as e:
                    logger.warning(f"Failed to extract image on page {page_number}: {e}")
                continue

            # Text block
            if block.get("type") != 0:
                continue

            for line in block.get("lines", []):
                line_text = ""
                max_font_size = 0
                is_bold = False

                for span in line.get("spans", []):
                    span_text = span.get("text", "").strip()
                    if not span_text:
                        continue
                    line_text += span_text + " "
                    font_size = span.get("size", 12)
                    max_font_size = max(max_font_size, font_size)
                    font_name = span.get("font", "").lower()
                    if "bold" in font_name or "heavy" in font_name:
                        is_bold = True

                line_text = line_text.strip()
                if not line_text:
                    continue

                raw_text_parts.append(line_text)

                # --- Structural classification ---
                section_type, level = _classify_text_block(
                    line_text, max_font_size, is_bold, page_number
                )

                # Infer document title from the first heading
                if section_type == "heading" and inferred_title is None:
                    inferred_title = line_text

                # Check if this looks like a figure caption
                if _is_figure_caption(line_text):
                    # Attach caption to the most recent figure
                    if figures and not figures[-1]["caption"]:
                        figures[-1]["caption"] = line_text
                    # Also update the figure section
                    for sec in reversed(sections):
                        if sec["type"] == "figure" and not sec.get("caption"):
                            sec["caption"] = line_text
                            sec["text"] = line_text
                            break
                    continue

                sections.append({
                    "type": section_type,
                    "level": level,
                    "text": line_text,
                    "page": page_number,
                })

    doc.close()

    return {
        "title": inferred_title,
        "page_count": len(doc) if hasattr(doc, '__len__') else 0,
        "sections": sections,
        "figures": figures,
        "raw_text": "\n".join(raw_text_parts),
        "_figures_dir": figures_dir,  # Internal: for cleanup
    }


def _classify_text_block(text: str, font_size: float, is_bold: bool, page: int) -> Tuple[str, Optional[int]]:
    """
    Classify a text block as heading, subheading, or paragraph
    based on font size and style heuristics.
    """
    # Chapter-level heading: large font + bold, or starts with "Chapter"
    if font_size >= 18 or (is_bold and font_size >= 16):
        return "heading", 1

    if text.lower().startswith("chapter"):
        return "heading", 1

    # Subheading: medium-large font + bold
    if is_bold and font_size >= 13:
        return "subheading", 2

    # Numbered section pattern (e.g., "5.1 Newton's First Law")
    import re
    if re.match(r'^\d+\.\d+', text) and is_bold:
        return "subheading", 2

    if is_bold and len(text) < 100:
        return "subheading", 3

    return "paragraph", None


def _is_figure_caption(text: str) -> bool:
    """Check if text looks like a figure caption."""
    import re
    text_lower = text.lower().strip()
    return bool(re.match(r'^(fig(ure)?|diagram|illustration|table)\s*\.?\s*\d', text_lower))


# ------------------------------------------------------------------
# Phase 2 — Semantic Validation (Grok / xAI)
# ------------------------------------------------------------------

def validate_domain_with_grok(raw_text: str, expected_domain: str) -> Dict[str, Any]:
    """
    Use Grok (xAI) to validate whether the document is secondary-level science
    and classify its domain.
    
    Returns:
        {
            "is_valid": bool,
            "detected_domain": str,
            "confidence": float (0-1),
            "reason": str
        }
    """
    api_key = os.environ.get("XAI_API_KEY") or os.environ.get("GROK_API_KEY")
    if not api_key:
        logger.warning("No XAI_API_KEY found — skipping domain validation (pass-through)")
        return {
            "is_valid": True,
            "detected_domain": expected_domain or "Unknown",
            "confidence": 0.5,
            "reason": "Validation skipped — no API key configured",
        }

    try:
        from openai import OpenAI

        client = OpenAI(
            api_key=api_key,
            base_url="https://api.x.ai/v1",
        )

        # Use first ~3000 chars for classification (compute-saving)
        text_sample = raw_text[:3000]

        prompt = f"""You are a document classifier for an educational science platform.

Analyze the following text extracted from a PDF document and determine:
1. Is this secondary-level (middle school / high school) science content?
2. What specific domain does it belong to? Choose from: Physics, Chemistry, Biology, Space/Astronomy, General Science, or Non-Science.
3. How confident are you in this classification (0.0 to 1.0)?

The user expects the document to be about: "{expected_domain}"

Text sample:
\"\"\"
{text_sample}
\"\"\"

Respond in JSON format only:
{{
    "is_secondary_science": true/false,
    "detected_domain": "Physics/Chemistry/Biology/Space/General Science/Non-Science",
    "confidence": 0.0-1.0,
    "reason": "Brief explanation"
}}"""

        response = client.chat.completions.create(
            model="grok-3-mini-fast",
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"},
            temperature=0.1,
        )

        result = json.loads(response.choices[0].message.content)

        is_valid = result.get("is_secondary_science", False)
        detected = result.get("detected_domain", "Unknown")
        confidence = float(result.get("confidence", 0.0))

        # Domain matching gate: if user specified a domain, check if it matches
        if expected_domain and expected_domain.lower() != "any":
            domain_match = (detected.lower().startswith(expected_domain.lower()) or
                          expected_domain.lower().startswith(detected.lower()) or
                          detected.lower() == "general science")
            if not domain_match and confidence > 0.7:
                is_valid = False
                result["reason"] = (
                    f"Domain mismatch: expected '{expected_domain}', "
                    f"but document appears to be '{detected}'"
                )

        return {
            "is_valid": is_valid,
            "detected_domain": detected,
            "confidence": confidence,
            "reason": result.get("reason", ""),
        }

    except Exception as e:
        logger.error(f"Grok validation error: {e}")
        # Fail open — allow processing if Grok is unavailable
        return {
            "is_valid": True,
            "detected_domain": expected_domain or "Unknown",
            "confidence": 0.0,
            "reason": f"Validation error (fail-open): {str(e)}",
        }


# ------------------------------------------------------------------
# Phase 5 — Async Orchestration
# ------------------------------------------------------------------

def run_async_content_generation(app, job_id: str, concepts: List[Dict], raw_text: str):
    """
    Background thread that generates content in parallel:
      - Tutor script (Grok/Ollama)
      - Quiz (existing quiz_generator)
      - TTS (placeholder)
      - 3D assets (ComfyUI for missing assets)
    
    Updates the job store progressively so the client can poll for results.
    """
    with app.app_context():
        try:
            _jobs[job_id]["status"] = "processing"

            threads = []

            # --- Quiz Generation (parallel) ---
            def generate_quiz():
                try:
                    from modules.quiz_generator import generate_quiz_from_text
                    quiz_text = raw_text[:5000]  # Truncate for LLM context
                    quiz = generate_quiz_from_text(quiz_text)
                    _jobs[job_id]["quiz"] = quiz
                    _jobs[job_id]["quiz_status"] = "complete"
                    logger.info(f"Job {job_id}: Quiz generated ({len(quiz)} questions)")
                except Exception as e:
                    logger.error(f"Job {job_id}: Quiz generation failed: {e}")
                    _jobs[job_id]["quiz"] = []
                    _jobs[job_id]["quiz_status"] = "error"

            # --- Tutor Script Generation (parallel) ---
            def generate_tutor_script():
                try:
                    tutor_script = _generate_tutor_script_grok(concepts, raw_text)
                    _jobs[job_id]["tutor_script"] = tutor_script
                    _jobs[job_id]["tutor_status"] = "complete"
                    logger.info(f"Job {job_id}: Tutor script generated")
                except Exception as e:
                    logger.error(f"Job {job_id}: Tutor script failed: {e}")
                    _jobs[job_id]["tutor_script"] = ""
                    _jobs[job_id]["tutor_status"] = "error"

            # --- Summary Generation (parallel) ---
            def generate_summary():
                try:
                    from modules.quiz_generator import generate_summary_from_text
                    summary = generate_summary_from_text(raw_text)
                    _jobs[job_id]["summary"] = summary
                    _jobs[job_id]["summary_status"] = "complete"
                    logger.info(f"Job {job_id}: Summary generated")
                except Exception as e:
                    logger.error(f"Job {job_id}: Summary failed: {e}")
                    _jobs[job_id]["summary"] = ""
                    _jobs[job_id]["summary_status"] = "error"

            # --- 3D Asset Generation for missing assets (parallel) ---
            def generate_missing_assets():
                try:
                    from modules.asset_engine import asset_engine
                    asset_engine.app = app._get_current_object()
                    
                    gen_results = []
                    for concept in concepts:
                        if concept.get("asset_source") == "pending_generation":
                            gen_job = asset_engine.trigger_generation_fallback(concept, app)
                            if gen_job:
                                gen_results.append({
                                    "concept": concept["concept_title"],
                                    "generation_job_id": gen_job,
                                })
                    
                    _jobs[job_id]["asset_generation"] = gen_results
                    _jobs[job_id]["assets_status"] = "complete" if not gen_results else "generating"
                    logger.info(f"Job {job_id}: Asset generation queued for {len(gen_results)} concepts")
                except Exception as e:
                    logger.error(f"Job {job_id}: Asset generation failed: {e}")
                    _jobs[job_id]["asset_generation"] = []
                    _jobs[job_id]["assets_status"] = "error"

            # Launch all threads
            for fn in [generate_quiz, generate_tutor_script, generate_summary, generate_missing_assets]:
                t = threading.Thread(target=fn, daemon=True)
                threads.append(t)
                t.start()

            # Wait for all to complete
            for t in threads:
                t.join(timeout=300)  # 5 min max per task

            _jobs[job_id]["status"] = "complete"
            logger.info(f"Job {job_id}: All async tasks completed")

        except Exception as e:
            logger.error(f"Job {job_id}: Orchestration error: {e}")
            _jobs[job_id]["status"] = "error"
            _jobs[job_id]["error"] = str(e)


def _generate_tutor_script_grok(concepts: List[Dict], raw_text: str) -> str:
    """Generate a tutor-style explanation script using Grok."""
    api_key = os.environ.get("XAI_API_KEY") or os.environ.get("GROK_API_KEY")
    
    if not api_key:
        # Fallback to Ollama
        return _generate_tutor_script_ollama(concepts, raw_text)

    try:
        from openai import OpenAI
        client = OpenAI(api_key=api_key, base_url="https://api.x.ai/v1")

        concept_titles = [c["concept_title"] for c in concepts[:10]]
        text_sample = raw_text[:4000]

        prompt = f"""You are an engaging science tutor for high school students.

Based on the following educational content, create a clear and engaging tutor script 
that explains the key concepts as if you're teaching a student one-on-one.

Concepts covered: {', '.join(concept_titles)}

Content:
\"\"\"
{text_sample}
\"\"\"

Write the tutor script in a conversational but educational tone. 
Break it into sections matching the concepts. Keep it concise but thorough."""

        response = client.chat.completions.create(
            model="grok-3-mini-fast",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
        )

        return response.choices[0].message.content.strip()

    except Exception as e:
        logger.error(f"Grok tutor script error: {e}")
        return _generate_tutor_script_ollama(concepts, raw_text)


def _generate_tutor_script_ollama(concepts: List[Dict], raw_text: str) -> str:
    """Fallback: generate tutor script using Ollama."""
    try:
        import ollama

        concept_titles = [c["concept_title"] for c in concepts[:10]]
        text_sample = raw_text[:3000]

        prompt = f"""You are a science tutor. Create a brief, engaging explanation of these concepts:
{', '.join(concept_titles)}

Based on: {text_sample}

Write in a conversational, educational tone suitable for high school students."""

        response = ollama.chat(
            model='phi4',
            messages=[{'role': 'user', 'content': prompt}],
            options={'temperature': 0.7}
        )
        return response['message']['content'].strip()
    except Exception as e:
        logger.error(f"Ollama tutor fallback error: {e}")
        return "Tutor script generation unavailable."


# ------------------------------------------------------------------
# API Endpoints
# ------------------------------------------------------------------

@pdf_bp.route('/upload', methods=['POST'])
def upload_pdf():
    """
    POST /api/pdf/upload
    
    Multipart form data:
      - file: PDF file
      - domain: Expected domain (Physics/Chemistry/Biology/Space)
    
    Full pipeline: Extract → Validate → Chunk → Asset lookup → Async orchestration
    """
    # --- Validate request ---
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'}), 400

    file = request.files['file']
    if file.filename == '' or not file.filename.lower().endswith('.pdf'):
        return jsonify({'error': 'Invalid file — must be a PDF'}), 400

    domain = request.form.get('domain', 'any')

    # --- Save temp file ---
    temp_id = str(uuid.uuid4())
    output_dir = current_app.config.get('OUTPUT_DIR', 'temp_uploads')
    os.makedirs(output_dir, exist_ok=True)
    temp_path = os.path.join(output_dir, f"temp_pdf_{temp_id}.pdf")
    file.save(temp_path)

    try:
        # ============ PHASE 1: Structured PDF Ingestion ============
        logger.info(f"Phase 1: Extracting content from PDF ({file.filename})")
        doc_data = extract_pdf_content(temp_path)
        doc_data["page_count"] = doc_data.get("page_count", 0) or len(doc_data.get("sections", []))

        # Recalculate page count from sections if needed
        if doc_data["page_count"] == 0 and doc_data["sections"]:
            doc_data["page_count"] = max(s.get("page", 1) for s in doc_data["sections"])

        logger.info(f"Phase 1 complete: {len(doc_data['sections'])} sections, "
                    f"{len(doc_data['figures'])} figures, "
                    f"{doc_data['page_count']} pages")

        # ============ PHASE 2: Semantic Validation ============
        logger.info("Phase 2: Validating domain with Grok...")
        validation = validate_domain_with_grok(doc_data["raw_text"], domain)

        if not validation["is_valid"]:
            logger.info(f"Phase 2: Document REJECTED — {validation['reason']}")
            return jsonify({
                "success": False,
                "rejected": True,
                "reason": validation["reason"],
                "detected_domain": validation["detected_domain"],
                "confidence": validation["confidence"],
            }), 422

        logger.info(f"Phase 2 passed: domain={validation['detected_domain']}, "
                    f"confidence={validation['confidence']:.2f}")

        # ============ PHASE 3: Concept Chunking ============
        logger.info("Phase 3: Chunking by concept boundaries...")
        from modules.concept_chunker import chunk_by_concepts
        concepts = chunk_by_concepts(doc_data["sections"])
        logger.info(f"Phase 3 complete: {len(concepts)} concept chunks")

        # ============ PHASE 4: Hybrid Asset Resolution ============
        logger.info("Phase 4: Resolving assets for each concept...")
        from modules.asset_engine import asset_engine
        concepts = asset_engine.resolve_assets_for_concepts(concepts)

        db_hits = sum(1 for c in concepts if c.get("asset_source") == "database")
        pending = sum(1 for c in concepts if c.get("asset_source") == "pending_generation")
        logger.info(f"Phase 4 complete: {db_hits} DB hits, {pending} pending generation")

        # ============ PHASE 5: Async Orchestration ============
        job_id = str(uuid.uuid4())
        _jobs[job_id] = {
            "status": "queued",
            "quiz": None,
            "quiz_status": "pending",
            "tutor_script": None,
            "tutor_status": "pending",
            "summary": None,
            "summary_status": "pending",
            "asset_generation": [],
            "assets_status": "pending",
        }

        # Start async content generation in background
        logger.info(f"Phase 5: Starting async orchestration (job: {job_id})")
        thread = threading.Thread(
            target=run_async_content_generation,
            args=(current_app._get_current_object(), job_id, concepts, doc_data["raw_text"]),
            daemon=True,
        )
        thread.start()

        # ============ Build Response ============
        # Clean up internal fields before sending
        clean_figures = []
        for fig in doc_data["figures"]:
            clean_figures.append({
                "id": fig["id"],
                "caption": fig.get("caption", ""),
                "image_url": f"/api/pdf/figure/{temp_id}/{fig['id']}",
                "page": fig.get("page", 0),
            })

        # Clean concept data for response
        clean_concepts = []
        for c in concepts:
            clean_concepts.append({
                "concept_title": c["concept_title"],
                "content_text": c["content_text"],
                "keywords": c.get("keywords", []),
                "related_figures": c.get("related_figures", []),
                "page_range": c["page_range"],
                "assets": c.get("assets", []),
                "asset_source": c.get("asset_source", "none"),
            })

        response = {
            "success": True,
            "job_id": job_id,
            "document": {
                "title": doc_data["title"],
                "page_count": doc_data["page_count"],
                "domain": validation["detected_domain"],
                "domain_confidence": validation["confidence"],
                "sections": doc_data["sections"][:100],  # Cap for response size
                "figures": clean_figures,
                "raw_text": doc_data["raw_text"][:10000],  # Cap for response size
            },
            "concepts": clean_concepts,
            "validation": validation,
        }

        return jsonify(response), 200

    except Exception as e:
        import traceback
        traceback.print_exc()
        logger.error(f"PDF pipeline error: {e}")
        return jsonify({'error': str(e)}), 500

    finally:
        # Cleanup temp PDF
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except Exception:
                pass


@pdf_bp.route('/status/<job_id>', methods=['GET'])
def get_job_status(job_id: str):
    """
    GET /api/pdf/status/<job_id>
    
    Poll this endpoint to get progressive results from async orchestration.
    """
    if job_id not in _jobs:
        return jsonify({'error': 'Job not found'}), 404

    job = _jobs[job_id]
    return jsonify({
        "job_id": job_id,
        "status": job["status"],
        "quiz": job.get("quiz"),
        "quiz_status": job.get("quiz_status", "pending"),
        "tutor_script": job.get("tutor_script"),
        "tutor_status": job.get("tutor_status", "pending"),
        "summary": job.get("summary"),
        "summary_status": job.get("summary_status", "pending"),
        "asset_generation": job.get("asset_generation", []),
        "assets_status": job.get("assets_status", "pending"),
        "error": job.get("error"),
    }), 200


@pdf_bp.route('/figure/<temp_id>/<fig_id>', methods=['GET'])
def get_figure(temp_id: str, fig_id: str):
    """
    GET /api/pdf/figure/<temp_id>/<fig_id>
    
    Serve an extracted figure image.
    """
    figures_dir = os.path.join(tempfile.gettempdir(), "stellar_pdf_figures", temp_id)
    
    # Try common extensions
    for ext in ['png', 'jpg', 'jpeg', 'bmp', 'gif']:
        fig_path = os.path.join(figures_dir, f"{fig_id}.{ext}")
        if os.path.exists(fig_path):
            return send_file(fig_path, mimetype=f"image/{ext}")

    return jsonify({'error': 'Figure not found'}), 404
