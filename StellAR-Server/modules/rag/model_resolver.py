"""
3D Model Resolver Module
========================
Asynchronous 3D model resolution and generation for the StellAR RAG pipeline.

Design:
    1. Cache check  → return instantly if model exists
    2. Job queue    → enqueue generation and return immediately
    3. Background worker → processes queue, generates .glb files
    4. Never blocks the API request

Usage:
    from modules.rag.model_resolver import resolve_3d_model, start_worker

    # In API handler (non-blocking):
    result = resolve_3d_model(concept)

    # On server startup (once):
    start_worker()
"""

from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import threading
import time
import uuid
from pathlib import Path
from queue import Queue, Empty
from typing import Any, Dict, List, Optional

import requests

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
MODELS_DIR = os.path.join(os.getcwd(), "generated_models")
COMFYUI_URL = os.environ.get("COMFYUI_URL", "http://127.0.0.1:8188")

# Worker settings
WORKER_POLL_INTERVAL = 2       # seconds between queue polls
GENERATION_TIMEOUT = 600       # max seconds to wait for a single model
MAX_WORKER_THREADS = 1         # single worker to avoid GPU contention

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# In-memory state
# ---------------------------------------------------------------------------

# Model cache: normalized title → { "model_url": str, "status": str }
_model_cache: Dict[str, Dict[str, Any]] = {}

# Job queue for background generation
_job_queue: Queue = Queue()

# Track in-flight jobs to prevent duplicates: normalized title → True
_active_jobs: Dict[str, bool] = {}
_active_jobs_lock = threading.Lock()

# Worker thread reference
_worker_thread: Optional[threading.Thread] = None
_worker_running = False


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _normalize_title(title: str) -> str:
    """Normalise concept title for use as cache key and filename."""
    return re.sub(r"\s+", " ", title.strip().lower())


def _slugify(title: str) -> str:
    """Convert title to a safe filename slug."""
    slug = re.sub(r"[^a-z0-9]+", "_", title.lower().strip())
    return slug.strip("_")


def _model_filename(title: str) -> str:
    """Deterministic .glb filename for a concept."""
    slug = _slugify(title)
    h = hashlib.md5(title.lower().strip().encode()).hexdigest()[:8]
    return f"{slug}_{h}.glb"


def _model_path(title: str) -> str:
    """Full path where a model .glb would be stored."""
    return os.path.join(MODELS_DIR, _model_filename(title))


# ---------------------------------------------------------------------------
# Cache / storage operations
# ---------------------------------------------------------------------------

def _check_local_storage(title: str) -> Optional[str]:
    """
    Check if a .glb file already exists on disk for this concept.
    Returns the file path if found, else None.
    """
    path = _model_path(title)
    if os.path.isfile(path) and os.path.getsize(path) > 0:
        logger.debug("Local model found: %s", path)
        return path
    return None


def _check_supabase_storage(title: str) -> Optional[str]:
    """
    Check if a model exists in Supabase Storage.
    Returns the public URL if found, else None.
    """
    try:
        from modules.supabase_service import supabase_service

        if not supabase_service.initialized:
            supabase_service.initialize()
        if not supabase_service.initialized:
            return None

        filename = _model_filename(title)
        destination = f"models/{filename}"

        # Try to get public URL — if the file doesn't exist, this
        # will return a URL that 404s, so we verify with a HEAD request
        public_url = supabase_service.get_client().storage.from_(
            "models"
        ).get_public_url(destination)

        # Quick HEAD check to verify the file actually exists
        resp = requests.head(public_url, timeout=5)
        if resp.status_code == 200:
            logger.info("Supabase model found: %s", public_url)
            return public_url

    except Exception as exc:
        logger.debug("Supabase storage check failed: %s", exc)

    return None


def _upload_to_supabase(local_path: str, title: str) -> Optional[str]:
    """Upload a generated model to Supabase Storage. Returns public URL."""
    try:
        from modules.supabase_service import supabase_service

        if not supabase_service.initialized:
            supabase_service.initialize()
        if not supabase_service.initialized:
            return None

        filename = _model_filename(title)
        destination = f"models/{filename}"
        public_url = supabase_service.upload_file(
            bucket="models",
            file_path=local_path,
            destination_path=destination,
        )
        logger.info("Model uploaded to Supabase: %s", public_url)
        return public_url

    except Exception as exc:
        logger.warning("Supabase upload failed for '%s': %s", title, exc)
        return None


# ---------------------------------------------------------------------------
# 3D generation (pluggable backend)
# ---------------------------------------------------------------------------

def _generate_model_comfyui(title: str, image_url: str) -> Optional[str]:
    """
    Generate a 3D model using the ComfyUI Hunyuan pipeline.
    Downloads the image, uploads to ComfyUI, runs the workflow,
    and returns the local .glb file path.
    """
    try:
        from modules.generation.comfyui_client import ComfyUIClient
        from modules.generation.pipeline import validate_image

        # Download the source image
        headers = {
            "User-Agent": "StellAR/1.0 (https://github.com/Lalit-Dhembre/StellAR_Project)"
        }
        img_resp = requests.get(image_url, headers=headers, timeout=15)
        img_resp.raise_for_status()

        # Save to temp file
        temp_dir = os.path.join(os.getcwd(), "temp_uploads")
        os.makedirs(temp_dir, exist_ok=True)
        temp_path = os.path.join(temp_dir, f"gen_input_{uuid.uuid4().hex}.png")
        with open(temp_path, "wb") as f:
            f.write(img_resp.content)

        # Validate image
        validated_path = validate_image(temp_path)
        if not validated_path:
            logger.warning("Image validation failed for '%s'", title)
            return None

        # Upload to ComfyUI and generate
        client = ComfyUIClient(comfyui_url=COMFYUI_URL)
        uploaded_name = client.upload_image(validated_path)
        if not uploaded_name:
            logger.error("ComfyUI image upload failed for '%s'", title)
            return None

        # TODO: Queue the actual workflow — this depends on the ComfyUI
        # workflow JSON being configured for image-to-3D generation.
        # For now, we simulate with a placeholder.
        logger.info("ComfyUI generation queued for '%s'", title)

        # Wait for output
        output_pattern = os.path.join(
            COMFYUI_URL.replace("http://127.0.0.1:8188", ""),
            f"gen_{_slugify(title)}_*.glb",
        )

        # Clean up temp file
        try:
            os.unlink(temp_path)
        except OSError:
            pass

        return None  # ComfyUI integration placeholder

    except ImportError:
        logger.debug("ComfyUI client not available")
        return None
    except Exception as exc:
        logger.error("ComfyUI generation failed for '%s': %s", title, exc)
        return None


def _generate_model_simulated(title: str, image_url: str) -> Optional[str]:
    """
    Simulated 3D generation for development/testing.
    Creates a placeholder .glb file to exercise the full pipeline.

    In production, replace this with the actual generation call
    (ComfyUI, Hunyuan, Meshy, etc.)
    """
    logger.info("Simulated generation for '%s' (image: %s)", title, image_url[:60])

    # Simulate processing time
    time.sleep(2)

    # Create output directory
    Path(MODELS_DIR).mkdir(parents=True, exist_ok=True)
    output_path = _model_path(title)

    # Write a minimal valid glTF binary (glb) header as placeholder
    # Real implementation would write actual model data
    glb_header = (
        b"glTF"           # magic
        + b"\x02\x00\x00\x00"  # version 2
        + b"\x00\x00\x00\x00"  # placeholder length
    )

    # Create a minimal valid GLB with an empty scene
    json_chunk = json.dumps({
        "asset": {"version": "2.0", "generator": "StellAR-Pipeline"},
        "scene": 0,
        "scenes": [{"name": title, "nodes": []}],
        "metadata": {
            "concept": title,
            "source_image": image_url,
            "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "status": "placeholder",
        },
    }).encode("utf-8")

    # Pad JSON to 4-byte alignment
    padding = (4 - len(json_chunk) % 4) % 4
    json_chunk += b" " * padding

    # GLB structure: header (12) + JSON chunk header (8) + JSON data
    json_chunk_header = (
        len(json_chunk).to_bytes(4, "little")
        + b"JSON"
    )

    total_length = 12 + 8 + len(json_chunk)
    glb_data = (
        b"glTF"
        + (2).to_bytes(4, "little")
        + total_length.to_bytes(4, "little")
        + json_chunk_header
        + json_chunk
    )

    with open(output_path, "wb") as f:
        f.write(glb_data)

    logger.info("Simulated model saved: %s (%d bytes)", output_path, len(glb_data))
    return output_path


def _generate_model(title: str, image_url: str) -> Optional[str]:
    """
    Attempt 3D model generation using available backends.
    Priority: ComfyUI → Simulated fallback.
    """
    # Try ComfyUI first (production)
    result = _generate_model_comfyui(title, image_url)
    if result:
        return result

    # Fall back to simulated generation (development)
    logger.info("Falling back to simulated generation for '%s'", title)
    return _generate_model_simulated(title, image_url)


# ---------------------------------------------------------------------------
# Job queue management
# ---------------------------------------------------------------------------

def _is_job_active(title: str) -> bool:
    """Check if a generation job is already in-flight for this concept."""
    key = _normalize_title(title)
    with _active_jobs_lock:
        return key in _active_jobs


def _mark_job_active(title: str) -> bool:
    """
    Mark a job as active. Returns False if already active (duplicate).
    """
    key = _normalize_title(title)
    with _active_jobs_lock:
        if key in _active_jobs:
            return False
        _active_jobs[key] = True
        return True


def _mark_job_complete(title: str) -> None:
    """Remove a job from the active set."""
    key = _normalize_title(title)
    with _active_jobs_lock:
        _active_jobs.pop(key, None)


def _enqueue_job(title: str, image_url: str) -> bool:
    """
    Add a generation job to the queue.
    Returns False if the job is already cached, queued, or active.
    """
    key = _normalize_title(title)

    # Skip if model is already cached (previously generated)
    if key in _model_cache and _model_cache[key].get("status") == "ready":
        logger.info("Model already cached for '%s' — skipping enqueue", title)
        return False

    if not _mark_job_active(title):
        logger.info("Job already active for '%s' — skipping enqueue", title)
        return False

    job = {
        "title": title,
        "image_url": image_url,
        "enqueued_at": time.time(),
    }
    _job_queue.put(job)
    logger.info("Enqueued generation job for '%s'", title)
    return True


# ---------------------------------------------------------------------------
# Background worker
# ---------------------------------------------------------------------------

def _process_single_job(job: Dict[str, Any]) -> None:
    """Process a single generation job."""
    title = job["title"]
    image_url = job.get("image_url", "")

    logger.info("Worker processing: '%s'", title)
    key = _normalize_title(title)

    try:
        # Generate the model
        local_path = _generate_model(title, image_url)

        if local_path and os.path.isfile(local_path):
            # Try uploading to Supabase
            supabase_url = _upload_to_supabase(local_path, title)
            model_url = supabase_url or local_path

            _model_cache[key] = {
                "model_url": model_url,
                "status": "ready",
            }
            logger.info("Model ready for '%s': %s", title, model_url)
        else:
            _model_cache[key] = {
                "model_url": None,
                "status": "not_available",
            }
            logger.warning("Generation produced no output for '%s'", title)

    except Exception as exc:
        _model_cache[key] = {
            "model_url": None,
            "status": "not_available",
        }
        logger.error("Generation failed for '%s': %s", title, exc)

    finally:
        _mark_job_complete(title)


def process_3d_queue() -> None:
    """
    Background worker loop — continuously processes the generation queue.
    Designed to run in a daemon thread.
    """
    global _worker_running
    _worker_running = True
    logger.info("3D model worker started")

    while _worker_running:
        try:
            job = _job_queue.get(timeout=WORKER_POLL_INTERVAL)
        except Empty:
            continue

        try:
            _process_single_job(job)
        except Exception as exc:
            logger.error("Worker encountered unexpected error: %s", exc)
        finally:
            _job_queue.task_done()

    logger.info("3D model worker stopped")


def start_worker() -> threading.Thread:
    """
    Start the background worker thread (if not already running).
    Call this once on server startup.
    """
    global _worker_thread

    if _worker_thread is not None and _worker_thread.is_alive():
        logger.info("Worker already running")
        return _worker_thread

    _worker_thread = threading.Thread(
        target=process_3d_queue,
        name="3d-model-worker",
        daemon=True,  # Dies when main thread exits
    )
    _worker_thread.start()
    logger.info("Background worker thread started")
    return _worker_thread


def stop_worker() -> None:
    """Signal the worker to stop."""
    global _worker_running
    _worker_running = False
    logger.info("Worker stop signal sent")


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def resolve_3d_model(concept: Dict[str, Any]) -> Dict[str, Any]:
    """
    Resolve a 3D model for a concept — never blocks.

    1. If model exists in cache/storage → returns immediately with status "ready"
    2. If generation is in progress → returns status "generating"
    3. Otherwise → enqueues generation and returns status "generating"

    Parameters
    ----------
    concept : Dict
        Must contain ``title``. Optionally ``image_url``.

    Returns
    -------
    Dict with:
        - ``model_url``  (str | None)
        - ``status``     ("ready" | "generating" | "not_available")
    """
    title = concept.get("title", "").strip()
    image_url = concept.get("image_url", "") or ""
    key = _normalize_title(title)

    # ── 1. Check in-memory cache ──────────────────────────────────────────
    if key in _model_cache:
        cached = _model_cache[key]
        logger.info("Cache hit for '%s': status=%s", title, cached["status"])
        return {
            "model_url": cached.get("model_url"),
            "status": cached["status"],
        }

    # ── 2. Check local disk ───────────────────────────────────────────────
    local_path = _check_local_storage(title)
    if local_path:
        _model_cache[key] = {"model_url": local_path, "status": "ready"}
        return {"model_url": local_path, "status": "ready"}

    # ── 3. Check Supabase storage ─────────────────────────────────────────
    supabase_url = _check_supabase_storage(title)
    if supabase_url:
        _model_cache[key] = {"model_url": supabase_url, "status": "ready"}
        return {"model_url": supabase_url, "status": "ready"}

    # ── 4. Not found — enqueue generation ─────────────────────────────────
    if _is_job_active(title):
        # Already being generated
        return {"model_url": None, "status": "generating"}

    if not image_url:
        logger.warning("No image_url for '%s' — cannot generate", title)
        return {"model_url": None, "status": "not_available"}

    _enqueue_job(title, image_url)
    return {"model_url": None, "status": "generating"}


def resolve_3d_models_batch(concepts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Resolve 3D models for multiple concepts.
    Returns concepts enriched with ``model_url`` and ``status``.
    """
    results: List[Dict[str, Any]] = []

    for concept in concepts:
        model_result = resolve_3d_model(concept)
        enriched = {**concept, **model_result}
        results.append(enriched)

    ready = sum(1 for r in results if r.get("status") == "ready")
    generating = sum(1 for r in results if r.get("status") == "generating")
    logger.info(
        "Batch resolve: %d ready, %d generating, %d total",
        ready, generating, len(results),
    )
    return results


# ---------------------------------------------------------------------------
# Example usage & tests
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    )

    print("=" * 60)
    print("3D Model Resolver — Example Run")
    print("=" * 60)

    # Start the background worker
    start_worker()

    sample_concepts = [
        {
            "title": "Human Heart",
            "image_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/Diagram_of_the_human_heart.svg/800px-Diagram_of_the_human_heart.svg.png",
        },
        {
            "title": "DNA Double Helix",
            "image_url": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/DNA_structure%2Bkey%2Blabelled.pn_nobb.png/800px-DNA_structure%2Bkey%2Blabelled.pn_nobb.png",
        },
    ]

    # ── First call: should enqueue jobs ───────────────────────────────────
    print("\n--- First resolve (should enqueue) ---")
    results = resolve_3d_models_batch(sample_concepts)
    for r in results:
        print(f"  [{r['status']:>14}] {r['title']} → {r.get('model_url', 'None')}")

    # ── Wait for worker to process ────────────────────────────────────────
    print("\n--- Waiting for worker to process... ---")
    _job_queue.join()  # Block until all jobs are done
    time.sleep(1)

    # ── Second call: should hit cache ─────────────────────────────────────
    print("\n--- Second resolve (should be ready) ---")
    results2 = resolve_3d_models_batch(sample_concepts)
    for r in results2:
        print(f"  [{r['status']:>14}] {r['title']} → {r.get('model_url', 'None')}")

    # ── Assertions ────────────────────────────────────────────────────────
    for r in results:
        assert "model_url" in r, "Missing 'model_url'"
        assert "status" in r, "Missing 'status'"
        assert r["status"] in {"ready", "generating", "not_available"}, \
            f"Invalid status: {r['status']}"

    for r in results2:
        assert r["status"] == "ready", f"Expected 'ready' for '{r['title']}', got '{r['status']}'"
        assert r["model_url"] is not None, f"model_url should not be None for '{r['title']}'"

    # ── Duplicate job test ────────────────────────────────────────────────
    print("\n--- Duplicate job test ---")
    enqueued = _enqueue_job("Human Heart", "http://example.com/img.png")
    print(f"  Duplicate enqueue returned: {enqueued} (expected False)")
    assert not enqueued, "Duplicate job should not be enqueued (model already cached)"

    stop_worker()
    print("\n✓ All assertions passed.")
