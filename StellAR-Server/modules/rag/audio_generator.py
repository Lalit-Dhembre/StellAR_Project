"""
Audio Generator Module
======================
Generates spoken educational explanations for concepts using:
    1. Local Ollama LLM -> conversational script generation
    2. gTTS -> standard free text-to-speech synthesis
    3. Supabase Storage -> persistent audio hosting (with local fallback)

Caching ensures each concept is only generated once.

Usage:
    from modules.rag.audio_generator import generate_audio
    result = generate_audio(concept)
"""

from __future__ import annotations

import hashlib
import json
import logging
import os
import re
import time
from io import BytesIO
from pathlib import Path
from typing import Any, Dict, List, Optional

from gtts import gTTS

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
from modules.local_llm import OLLAMA_MODEL_NAME, extract_json_fragment, generate_with_ollama

# Audio file storage
AUDIO_DIR = os.path.join(os.getcwd(), "generated_audio")
SUPABASE_AUDIO_BUCKET = "audio"

MAX_SCRIPT_WORDS = 100
MAX_TTS_CHARACTERS = 2000
MAX_CHUNK_CONTEXT_CHARACTERS = 1400
BATCH_SCRIPT_TIMEOUT_SECONDS = int(os.environ.get("RAG_BATCH_SCRIPT_TIMEOUT_SECONDS", "60"))

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# In-memory audio cache (concept title -> result dict)
# ---------------------------------------------------------------------------
_audio_cache: Dict[str, Dict[str, Any]] = {}

# ---------------------------------------------------------------------------
# Script generation prompt
# ---------------------------------------------------------------------------

SCRIPT_SYSTEM_PROMPT = """\
You are a friendly teacher explaining a concept to a student.

CRITICAL RULES:
- Output ONLY the spoken explanation. Nothing else.
- Do NOT start with "Here is...", "Sure...", "Let me explain...", or any preamble.
- Do NOT describe what you are about to write. Just write it directly.
- Write 3 to 5 sentences ONLY.
- Use a warm, conversational tone a high-school student would understand.
- Start directly with an engaging hook about the topic.
- Keep under 100 words.
- No bullet points, lists, markdown, or formatting.
- Write natural spoken English — this will be read aloud by a text-to-speech engine.
"""

SCRIPT_USER_TEMPLATE = """\
Concept: {title}
Explanation: {short_explanation}
Relevant source chunk:
\"\"\"
{source_chunk}
\"\"\"

Write a short spoken explanation about this concept. Start directly with the explanation. /no_think
"""

BATCH_SCRIPT_SYSTEM_PROMPT = """\
You are a friendly teacher creating short spoken explanations for an educational AR app.

Return ONLY valid JSON in this exact shape:
{
  "scripts": [
    {"id": "concept-id", "script": "3 to 5 sentence spoken explanation"},
    {"id": "concept-id-2", "script": "3 to 5 sentence spoken explanation"}
  ]
}

CRITICAL RULES:
- Output one script for every provided concept id.
- Output ONLY JSON. No markdown, notes, or extra text.
- Each script must be 3 to 5 sentences and under 100 words.
- Write natural spoken English for text-to-speech.
- Start directly with the explanation, not with preambles like "Here is".
- Ground each script in the source chunk and the concept explanation.
"""

BATCH_SCRIPT_USER_TEMPLATE = """\
Source chunk:
\"\"\"
{source_chunk}
\"\"\"

Generate narration scripts for these concepts:
{concepts_json}

Return JSON only.
"""


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _slugify(text: str) -> str:
    """Convert a concept title to a safe filename slug."""
    text = text.lower().strip()
    text = re.sub(r"[^a-z0-9]+", "_", text)
    return text.strip("_")


def _concept_hash(title: str) -> str:
    """Short hash for cache deduplication."""
    return hashlib.md5(title.lower().strip().encode()).hexdigest()[:10]


def _audio_filename(title: str) -> str:
    """Generate a unique, deterministic filename for a concept's audio."""
    slug = _slugify(title)
    h = _concept_hash(title)
    return f"{slug}_{h}.mp3"


def _normalize_script(script: str) -> str:
    """Trim and normalize whitespace before sending text to TTS."""
    return re.sub(r"\s+", " ", script or "").strip()


def _clean_script(script: str) -> str:
    """
    Strip LLM meta-commentary, preamble, and prompt echoes from script output.
    Models like qwen3 often prefix with reasoning or 'Here is the explanation:'.
    """
    text = (script or "").strip()
    if not text:
        return text

    # Strip <think>...</think> blocks (some models emit even with think=false)
    text = re.sub(r"<think>[\s\S]*?</think>\s*", "", text).strip()

    # Strip common LLM meta-prefixes (greedy, case-insensitive)
    meta_patterns = [
        r"^(?:Here(?:'s| is) (?:a |the )?(?:short |brief )?(?:spoken )?(?:explanation|script|narration)[^.]*[.:]\s*)",
        r"^(?:Sure[!,.]?\s*(?:Here(?:'s| is)[^.]*[.:])?\s*)",
        r"^(?:Of course[!,.]?\s*(?:Here(?:'s| is)[^.]*[.:])?\s*)",
        r"^(?:Let me (?:explain|write|create)[^.]*[.:]\s*)",
        r"^(?:We are writing[^.]*[.:]\s*)",
        r"^(?:I(?:'ll| will) (?:write|explain|create)[^.]*[.:]\s*)",
        r'^(?:Script:\s*)',
        r'^(?:Explanation:\s*)',
        r'^(?:Narration:\s*)',
    ]
    for pattern in meta_patterns:
        text = re.sub(pattern, "", text, flags=re.IGNORECASE).strip()

    # Strip leading/trailing quotes if the model wrapped the whole output
    if (text.startswith('"') and text.endswith('"')) or \
       (text.startswith("'") and text.endswith("'")):
        text = text[1:-1].strip()

    return text


def _trim_chunk_context(text: str) -> str:
    """Trim chunk context to keep prompts focused and bounded."""
    normalized = re.sub(r"\s+", " ", text or "").strip()
    if len(normalized) <= MAX_CHUNK_CONTEXT_CHARACTERS:
        return normalized
    trimmed = normalized[:MAX_CHUNK_CONTEXT_CHARACTERS].rsplit(" ", 1)[0].rstrip(",;:")
    return (trimmed or normalized[:MAX_CHUNK_CONTEXT_CHARACTERS]).strip() + "..."


def _finalize_script(script: str) -> str:
    """Clean and cap a script before returning it to the pipeline."""
    cleaned = _clean_script(script)
    if not cleaned:
        return ""

    words = cleaned.split()
    if len(words) > MAX_SCRIPT_WORDS + 20:
        cleaned = " ".join(words[:MAX_SCRIPT_WORDS]).rstrip(".") + "."
        logger.info("Truncated script to %d words", MAX_SCRIPT_WORDS)

    return cleaned.strip()


def _build_fallback_script(title: str, explanation: str, source_chunk: str = "") -> str:
    """Create a deterministic spoken fallback when LLM generation fails."""
    supporting_text = explanation.strip() or _trim_chunk_context(source_chunk)
    if not supporting_text:
        supporting_text = f"{title} is an important concept worth exploring further."

    fallback = (
        f"Let's understand {title}. "
        f"{supporting_text} "
        f"This is an important concept worth exploring further."
    )
    return _finalize_script(fallback) or fallback.strip()


def _build_failure_result(script: str, error: str) -> Dict[str, Any]:
    """Return the public error shape required by the audio API."""
    return {
        "audio_url": None,
        "script": script,
        "error": error,
    }


# ---------------------------------------------------------------------------
# Script generation (local Ollama)
# ---------------------------------------------------------------------------

def generate_script(concept: Dict[str, Any]) -> str:
    """
    Generate a natural spoken explanation script for the concept using Ollama.

    Falls back to a simple template if the LLM call fails.
    """
    title = concept.get("title", "").strip()
    explanation = concept.get("short_explanation", "").strip()
    source_chunk = _trim_chunk_context(concept.get("source_chunk", ""))
    fallback = _build_fallback_script(title, explanation, source_chunk)

    try:
        raw_script = generate_with_ollama(
            prompt=SCRIPT_USER_TEMPLATE.format(
                title=title,
                short_explanation=explanation,
                source_chunk=source_chunk or "No additional chunk context provided.",
            ),
            system=SCRIPT_SYSTEM_PROMPT,
            model=OLLAMA_MODEL_NAME,
            options={
                "temperature": 0.4,
                "num_predict": 220,
            },
            timeout=30,
        )
        script = _finalize_script(raw_script)

        if not script or len(script.split()) < 10:
            logger.warning("LLM returned too-short script after cleaning, using fallback")
            return fallback

        logger.info("Generated script for '%s' (%d words): %s",
                     title, len(script.split()), script[:80])
        return script

    except Exception as exc:
        logger.error("Script generation failed for '%s': %s", title, exc)
        return fallback


def _parse_script_batch_response(raw_output: str) -> Dict[str, str]:
    """Parse a JSON batch of concept scripts into an id->script map."""
    json_fragment = extract_json_fragment(raw_output)
    if not json_fragment:
        return {}

    try:
        data = json.loads(json_fragment)
    except json.JSONDecodeError:
        return {}

    parsed: Dict[str, str] = {}

    def _store(concept_id: Any, script: Any) -> None:
        key = str(concept_id or "").strip()
        if not key or not isinstance(script, str):
            return
        finalized = _finalize_script(script)
        if finalized:
            parsed[key] = finalized

    if isinstance(data, dict):
        scripts = data.get("scripts")
        if isinstance(scripts, list):
            for item in scripts:
                if not isinstance(item, dict):
                    continue
                _store(item.get("id"), item.get("script"))
        else:
            for concept_id, script in data.items():
                if isinstance(script, dict):
                    _store(concept_id, script.get("script"))
                else:
                    _store(concept_id, script)
    elif isinstance(data, list):
        for item in data:
            if not isinstance(item, dict):
                continue
            _store(item.get("id"), item.get("script"))

    return parsed


def generate_scripts_for_chunk(
    concepts: List[Dict[str, Any]],
    chunk_text: Optional[str] = None,
) -> Dict[str, str]:
    """
    Generate narration scripts for a chunk's concepts in one LLM call.

    Falls back to deterministic per-concept scripts if the batch call fails.
    """
    if not concepts:
        return {}

    chunk_context = _trim_chunk_context(
        chunk_text or next((concept.get("source_chunk", "") for concept in concepts if concept.get("source_chunk")), "")
    )

    concept_payload = []
    fallback_scripts: Dict[str, str] = {}

    for concept in concepts:
        concept_id = str(concept.get("id", "")).strip()
        title = concept.get("title", "").strip()
        explanation = concept.get("short_explanation", "").strip()
        if not concept_id or not title:
            continue

        concept_payload.append({
            "id": concept_id,
            "title": title,
            "short_explanation": explanation,
        })
        fallback_scripts[concept_id] = _build_fallback_script(title, explanation, chunk_context)

    if not concept_payload:
        return {}

    try:
        raw_output = generate_with_ollama(
            prompt=BATCH_SCRIPT_USER_TEMPLATE.format(
                source_chunk=chunk_context or "No additional chunk context provided.",
                concepts_json=json.dumps(concept_payload, ensure_ascii=True, indent=2),
            ),
            system=BATCH_SCRIPT_SYSTEM_PROMPT,
            model=OLLAMA_MODEL_NAME,
            format="json",
            options={
                "temperature": 0.25,
                "num_predict": max(400, len(concept_payload) * 220),
            },
            timeout=max(BATCH_SCRIPT_TIMEOUT_SECONDS, 20 + len(concept_payload) * 8),
        )
        parsed_scripts = _parse_script_batch_response(raw_output)
    except Exception as exc:
        logger.error("Chunk script batch generation failed: %s", exc)
        parsed_scripts = {}

    results: Dict[str, str] = {}
    for concept in concept_payload:
        concept_id = concept["id"]
        candidate = parsed_scripts.get(concept_id, "")
        if not candidate or len(candidate.split()) < 10:
            logger.warning(
                "Using fallback script for concept '%s' after chunk batch generation",
                concept.get("title", concept_id),
            )
            candidate = fallback_scripts[concept_id]
        results[concept_id] = candidate

    return results


# ---------------------------------------------------------------------------
# gTTS
# ---------------------------------------------------------------------------

def _synthesize_speech(script: str) -> tuple[Optional[bytes], Optional[str]]:
    """
    Call gTTS to convert script text into MP3 audio bytes.
    Returns the raw audio bytes, or None on failure.
    """
    normalized_script = _normalize_script(script)
    if not normalized_script:
        error = "Skipping TTS request: script is empty after normalization"
        logger.error(error)
        return None, error

    if len(normalized_script) >= MAX_TTS_CHARACTERS:
        error = (
            "Skipping TTS request: script length "
            f"{len(normalized_script)} exceeds limit of {MAX_TTS_CHARACTERS - 1} characters"
        )
        logger.error(
            "Skipping TTS request: script length %d exceeds limit of %d characters",
            len(normalized_script),
            MAX_TTS_CHARACTERS - 1,
        )
        return None, error

    try:
        logger.info("Starting standard TTS synthesis using gTTS...")
        # We use standard English TTS ('en').
        tts = gTTS(text=normalized_script, lang='en', slow=False)
        fp = BytesIO()
        tts.write_to_fp(fp)
        audio_bytes = fp.getvalue()

        if not audio_bytes:
            error = "gTTS returned an empty audio response"
            logger.error(error)
            return None, error

        logger.info("TTS synthesis complete: %d bytes", len(audio_bytes))
        return audio_bytes, None

    except Exception as exc:
        error = f"gTTS failure: {str(exc)}"
        logger.error(error)
        return None, error


# ---------------------------------------------------------------------------
# Audio storage
# ---------------------------------------------------------------------------

def _save_audio_locally(audio_bytes: bytes, filename: str) -> str:
    """Save audio bytes to the local filesystem. Returns the file path."""
    if not audio_bytes:
        raise ValueError("Cannot save empty audio content")

    Path(AUDIO_DIR).mkdir(parents=True, exist_ok=True)
    filepath = os.path.join(AUDIO_DIR, filename)

    with open(filepath, "wb") as file_obj:
        file_obj.write(audio_bytes)

    if os.path.getsize(filepath) == 0:
        raise ValueError(f"Saved audio file is empty: {filepath}")

    logger.info("Audio saved locally: %s", filepath)
    return filepath


def _upload_to_supabase(local_path: str, filename: str) -> Optional[str]:
    """
    Upload audio file to Supabase Storage and return the public URL.
    Returns None if Supabase is unavailable.
    """
    try:
        from modules.supabase_service import supabase_service

        if not supabase_service.initialized:
            supabase_service.initialize()

        if not supabase_service.initialized:
            logger.warning("Supabase not initialized - audio stays local only")
            return None

        destination = f"concepts/{filename}"
        public_url = supabase_service.upload_file(
            bucket=SUPABASE_AUDIO_BUCKET,
            file_path=local_path,
            destination_path=destination,
        )
        logger.info("Audio uploaded to Supabase: %s", public_url)
        return public_url

    except Exception as exc:
        logger.warning("Supabase upload failed: %s", exc)
        return None


def _store_audio(audio_bytes: bytes, filename: str) -> str:
    """
    Save audio locally and attempt Supabase upload.
    Returns the best available URL (Supabase public URL or local path).
    """
    local_path = _save_audio_locally(audio_bytes, filename)
    supabase_url = _upload_to_supabase(local_path, filename)
    return supabase_url or local_path


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def generate_audio(concept: Dict[str, Any]) -> Dict[str, Any]:
    """
    Generate an audio explanation for a concept.

    Parameters
    ----------
    concept : Dict
        Must contain at least ``title`` and ``short_explanation``.

    Returns
    -------
    Dict with:
        - ``audio_url``  (str | None) - URL or local path to the .mp3 file
        - ``script``     (str)        - the spoken text that was synthesized
        - ``error``      (str | None) - synthesis or validation error message
    """
    title = concept.get("title", "").strip()
    cache_key = title.lower()

    if cache_key in _audio_cache:
        logger.info("Audio cache hit for '%s'", title)
        return _audio_cache[cache_key].copy()

    script = _normalize_script(generate_script(concept))
    logger.info("Script for '%s': %s", title, script[:80])

    if not script:
        result = _build_failure_result("", "Script is empty after normalization.")
        _audio_cache[cache_key] = result.copy()
        return result

    if len(script) >= MAX_TTS_CHARACTERS:
        result = _build_failure_result(
            script,
            f"Script exceeds length limit ({len(script)} characters).",
        )
        _audio_cache[cache_key] = result.copy()
        return result

    audio_bytes, synthesis_error = _synthesize_speech(script)
    if audio_bytes is None:
        result = _build_failure_result(
            script,
            synthesis_error or "Audio synthesis failed. The script is available for text display.",
        )
        _audio_cache[cache_key] = result.copy()
        return result

    try:
        filename = _audio_filename(title)
        audio_url = _store_audio(audio_bytes, filename)
    except Exception as exc:
        logger.error("Failed to store audio for '%s': %s", title, exc)
        result = _build_failure_result(script, f"Audio storage failed: {exc}")
        _audio_cache[cache_key] = result.copy()
        return result

    result = {
        "audio_url": audio_url,
        "script": script,
        "error": None,
    }
    _audio_cache[cache_key] = result.copy()

    logger.info("Audio generation complete for '%s'", title)
    return result


def generate_audio_batch(concepts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Generate audio for multiple concepts sequentially.

    Parameters
    ----------
    concepts : List[Dict]
        List of concept dicts.

    Returns
    -------
    List[Dict]
        Each concept enriched with ``audio_url`` and ``script``.
    """
    if not concepts:
        return []

    results: List[Dict[str, Any]] = []

    for idx, concept in enumerate(concepts):
        logger.info(
            "Generating audio %d/%d: '%s'",
            idx + 1,
            len(concepts),
            concept.get("title", "?"),
        )
        audio_result = generate_audio(concept)
        enriched = {**concept, **audio_result}
        results.append(enriched)

    generated = sum(1 for result in results if result.get("audio_url"))
    logger.info(
        "Audio batch complete: %d/%d concepts have audio",
        generated,
        len(results),
    )
    return results


# ---------------------------------------------------------------------------
# Example usage & tests
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    try:
        from dotenv import load_dotenv

        load_dotenv()
    except ImportError:
        pass

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    )

    sample_concepts = [
        {
            "id": "test-001",
            "title": "Human Heart",
            "type": "object",
            "confidence": 0.95,
            "keywords": ["heart", "organ", "chambers", "ventricle", "circulatory"],
            "short_explanation": (
                "The human heart is a muscular organ with four chambers "
                "that pumps blood through the circulatory system."
            ),
        },
        {
            "id": "test-002",
            "title": "Photosynthesis",
            "type": "process",
            "confidence": 0.90,
            "keywords": ["chloroplast", "sunlight", "oxygen", "calvin cycle", "thylakoid"],
            "short_explanation": (
                "Photosynthesis converts sunlight into chemical energy in "
                "the chloroplasts of plant cells."
            ),
        },
    ]

    print("=" * 60)
    print("Audio Generation - Example Run")
    print("=" * 60)

    results = generate_audio_batch(sample_concepts)

    for result in results:
        print(f"\n{'-' * 50}")
        print(f"  Title  : {result['title']}")
        print(f"  Audio  : {result.get('audio_url', 'None')}")
        print(f"  Script : {result.get('script', 'N/A')[:120]}...")
        if result.get("error"):
            print(f"  Error  : {result['error']}")

    for result in results:
        assert "script" in result, "Missing 'script'"
        assert "audio_url" in result, "Missing 'audio_url'"
        assert "error" in result, "Missing 'error'"
        assert isinstance(result["script"], str), "Script should be a string"
        assert result.get("audio_url") is not None, "Audio URL should be generated"
        assert result.get("error") is None, "There should be no errors"

    print("\nAll assertions passed.")
