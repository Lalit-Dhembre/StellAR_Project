"""
Concept Extractor Module
========================
Extracts visualizable educational concepts from text chunks using Groq LLM.
Each concept is returned as a structured dict suitable for downstream 3D
model retrieval / generation.

Usage:
    from modules.rag.concept_extractor import extract_concepts
    concepts = extract_concepts(chunks)
"""

from __future__ import annotations

import json
import logging
import os
import uuid
from typing import Any, Dict, List, Optional

from groq import Groq

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
# Reuse the same model the rest of the project uses, with env-var override
GROQ_MODEL_NAME = os.environ.get("GROQ_MODEL_NAME", "llama-3.1-8b-instant")
MAX_CONCEPTS_PER_CHUNK = 8         # Increased cap dynamically due to batching
MIN_CONFIDENCE = 0.7               # Concepts below this are discarded
MAX_RETRIES = 2                    # Retry budget when JSON parsing fails
LLM_TEMPERATURE = 0.3             # Low temp → more deterministic extraction
LLM_MAX_TOKENS = 4096             # More room for batched extracted concepts

# Module-level logger
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Groq client helper (mirrors modules/api/domain_validator.py)
# ---------------------------------------------------------------------------

def _get_groq_client() -> Groq:
    """
    Initialise and return a Groq client.
    Reads GROQ_API_KEY from the environment — raises ValueError if unset.
    """
    api_key = os.environ.get("GROQ_API_KEY")
    if not api_key:
        raise ValueError("GROQ_API_KEY environment variable is not set")
    return Groq(api_key=api_key)

# ---------------------------------------------------------------------------
# Prompt templates
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = """\
You are an expert educational content analyzer for an augmented-reality \
learning application.

Your task is to extract ONLY important, visualizable concepts from the \
provided text.

### What counts as a visualizable concept
- Something that can be represented as a 3D model, diagram, structure, or \
  animated process.
- Real physical objects, organisms, celestial bodies, molecules, organs, \
  machines, geological formations, etc.
- Processes that have clear visual stages (e.g. water cycle, cell division).

### What does NOT count
- Purely abstract ideas with no visual form (e.g. "democracy", "justice").
- Trivial or generic terms (e.g. "text", "example", "information").
- Names of people, dates, or numerical constants.
- Overlapping concepts that describe the same core idea in different words.
- Parent/child duplicates where one concept is just a sub-part, synonym, or
  narrower restatement of another returned concept.

### Output rules
1. Return **ONLY** a valid JSON object with a single key "concepts" whose \
   value is an array.
2. Each element must be an object with exactly these keys:
   - "title"             : string — short concept name (2-5 words)
   - "type"              : string — one of "object", "process", "structure", \
                           "diagram"
   - "confidence"        : number — 0.0 to 1.0
   - "keywords"          : array  — exactly 5 relevant search terms
   - "short_explanation" : string — 2-3 sentence description
3. Extract at most 8 concepts. Fewer is fine if the text doesn't contain \
   enough visualizable content.
4. If the text contains NO visualizable concepts, return: {"concepts": []}
5. Do NOT hallucinate concepts that are not clearly supported by the text.
6. Avoid overlapping concepts. Each concept should represent a DISTINCT idea.
7. If two candidates are closely related, keep only the broader or more
   instructionally useful one.
8. Do not return synonyms, paraphrases, or near-duplicates of the same concept.
"""

USER_PROMPT_TEMPLATE = """\
Text:
\"\"\"
{chunk}
\"\"\"

Extract the visualizable concepts from the text above.
Avoid overlapping concepts.
Each concept should represent a DISTINCT idea.
Return ONLY valid JSON.
"""


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _parse_concepts_json(raw: str) -> List[Dict[str, Any]]:
    """
    Parse the raw LLM output into a list of concept dicts.

    Handles edge cases:
    - Output wrapped in markdown code fences
    - Output is a dict with a wrapper key (e.g. {"concepts": [...]})
    - Output contains leading/trailing garbage text around JSON
    """
    text = raw.strip()

    # Strip markdown code fences if present
    if text.startswith("```"):
        first_newline = text.index("\n")
        text = text[first_newline + 1:]
        if text.rstrip().endswith("```"):
            text = text.rstrip()[:-3].rstrip()

    # Try direct parse first
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        # Fallback: locate the outermost [ ... ] or { ... } in the string
        start = text.find("[")
        end = text.rfind("]")
        if start != -1 and end != -1 and end > start:
            data = json.loads(text[start : end + 1])
        else:
            start = text.find("{")
            end = text.rfind("}")
            if start != -1 and end != -1 and end > start:
                data = json.loads(text[start : end + 1])
            else:
                raise ValueError(f"No JSON found in LLM output: {text[:200]}")

    # If the model returned a wrapper object, unwrap it
    if isinstance(data, dict):
        for key in ("concepts", "results", "data", "items"):
            if key in data and isinstance(data[key], list):
                data = data[key]
                break
        else:
            # Single concept returned as dict → wrap in list
            if "title" in data:
                data = [data]
            else:
                data = []

    if not isinstance(data, list):
        raise ValueError(f"Expected a JSON array, got {type(data).__name__}")

    return data


def _validate_concept(concept: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """
    Validate and normalise a single concept dict.
    Returns None if the concept is malformed or below the confidence threshold.
    """
    # Required keys check
    required = {"title", "type", "confidence", "keywords", "short_explanation"}
    if not required.issubset(concept.keys()):
        missing = required - concept.keys()
        logger.debug("Concept missing keys %s — skipping: %s", missing, concept)
        return None

    # Type must be one of the allowed values
    allowed_types = {"object", "process", "structure", "diagram"}
    if concept["type"] not in allowed_types:
        logger.debug("Invalid concept type '%s' — skipping", concept["type"])
        return None

    # Confidence must be numeric and >= threshold
    try:
        confidence = float(concept["confidence"])
    except (TypeError, ValueError):
        logger.debug("Non-numeric confidence — skipping: %s", concept)
        return None

    if confidence < MIN_CONFIDENCE:
        logger.debug(
            "Low confidence (%.2f < %.2f) — filtering out: %s",
            confidence, MIN_CONFIDENCE, concept.get("title"),
        )
        return None

    # Keywords must be a list of strings
    keywords = concept.get("keywords", [])
    if not isinstance(keywords, list):
        keywords = []
    keywords = [str(k) for k in keywords if k][:5]  # cap at 5

    # Build the clean, validated concept
    return {
        "id": str(uuid.uuid4()),
        "title": str(concept["title"]).strip(),
        "type": concept["type"],
        "confidence": round(confidence, 2),
        "keywords": keywords,
        "short_explanation": str(concept.get("short_explanation", "")).strip(),
    }


def _extract_from_chunk(chunk: str) -> List[Dict[str, Any]]:
    """
    Call the Groq LLM for a single chunk and return validated concepts.
    Retries up to MAX_RETRIES times on JSON parse failures.
    """
    last_error: Optional[Exception] = None

    for attempt in range(1, MAX_RETRIES + 2):  # +2 because range is exclusive
        try:
            logger.info(
                "Groq call attempt %d/%d for chunk (%.40s…)",
                attempt, MAX_RETRIES + 1, chunk,
            )

            # Initialise Groq client (consistent with project's domain_validator.py)
            client = _get_groq_client()

            response = client.chat.completions.create(
                model=GROQ_MODEL_NAME,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user",   "content": USER_PROMPT_TEMPLATE.format(chunk=chunk)},
                ],
                temperature=LLM_TEMPERATURE,
                max_completion_tokens=LLM_MAX_TOKENS,
                response_format={"type": "json_object"},
            )

            raw_output = response.choices[0].message.content.strip()
            logger.debug("Raw Groq output: %s", raw_output[:300])

            # Parse the JSON from the LLM response
            raw_concepts = _parse_concepts_json(raw_output)

            # Validate each concept and filter
            validated: List[Dict[str, Any]] = []
            for raw_concept in raw_concepts[:MAX_CONCEPTS_PER_CHUNK]:
                clean = _validate_concept(raw_concept)
                if clean is not None:
                    validated.append(clean)

            logger.info(
                "Extracted %d valid concept(s) from chunk", len(validated),
            )
            return validated

        except json.JSONDecodeError as exc:
            last_error = exc
            logger.warning(
                "JSON parse failed on attempt %d: %s", attempt, exc,
            )

        except Exception as exc:
            last_error = exc
            logger.error(
                "Groq call failed on attempt %d: %s", attempt, exc,
            )
            # Don't retry on non-parse errors (e.g. auth failure, rate limit)
            break

    # All retries exhausted
    logger.error(
        "All %d attempts failed for chunk — returning empty. Last error: %s",
        MAX_RETRIES + 1, last_error,
    )
    return []


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def extract_concepts(chunks: List[str]) -> List[Dict[str, Any]]:
    """
    Extract visualizable concepts from a list of text chunks.
    Batches chunks aggressively to minimize 429 API Rate Limits.
    """
    if not chunks:
        logger.warning("extract_concepts called with empty chunk list")
        return []

    # Batching logic: Combine chunks into larger megachunks up to ~4000 chars
    batched_chunks = []
    current_batch = []
    current_length = 0
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk: continue
        
        if current_length + len(chunk) > 4000 and current_batch:
            batched_chunks.append("\n\n".join(current_batch))
            current_batch = [chunk]
            current_length = len(chunk)
        else:
            current_batch.append(chunk)
            current_length += len(chunk)
            
    if current_batch:
        batched_chunks.append("\n\n".join(current_batch))

    all_concepts: List[Dict[str, Any]] = []

    for idx, mega_chunk in enumerate(batched_chunks):
        logger.info(
            "Processing batched Mega-Chunk %d/%d (%d chars) to save LLM calls",
            idx + 1, len(batched_chunks), len(mega_chunk)
        )
        concepts = _extract_from_chunk(mega_chunk)
        all_concepts.extend(concepts)

    logger.info(
        "Concept extraction complete: %d concept(s) from %d chunks (bundled into %d API calls)",
        len(all_concepts), len(chunks), len(batched_chunks)
    )
    return all_concepts


# ---------------------------------------------------------------------------
# Example usage & simple tests
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    # Load .env so GROQ_API_KEY is available when running standalone
    try:
        from dotenv import load_dotenv
        load_dotenv()
    except ImportError:
        pass  # dotenv not required if env vars are set externally

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    )

    # ----- Example: real extraction from sample educational text -----------
    sample_chunks = [
        (
            "The human heart is a muscular organ roughly the size of a fist. "
            "It has four chambers: the left atrium, right atrium, left ventricle, "
            "and right ventricle. The heart pumps blood through the circulatory "
            "system, delivering oxygen and nutrients to every cell in the body. "
            "Deoxygenated blood returns to the heart through veins, enters the "
            "right atrium, and is pumped to the lungs for gas exchange."
        ),
        (
            "Photosynthesis is the process by which green plants convert sunlight "
            "into chemical energy. It occurs primarily in the chloroplasts of leaf "
            "cells. The process has two main stages: the light-dependent reactions, "
            "which take place in the thylakoid membranes, and the Calvin cycle, "
            "which occurs in the stroma. Water molecules are split during the "
            "light reactions, releasing oxygen as a byproduct."
        ),
    ]

    print("=" * 60)
    print("Concept Extraction — Example Run (Groq)")
    print("=" * 60)

    results = extract_concepts(sample_chunks)

    print(f"\nTotal concepts extracted: {len(results)}\n")
    for concept in results:
        print(json.dumps(concept, indent=2))
        print()

    # ----- Assertions (basic sanity checks) --------------------------------
    for c in results:
        assert "id" in c,               "Missing 'id'"
        assert "title" in c,            "Missing 'title'"
        assert "type" in c,             "Missing 'type'"
        assert "confidence" in c,       "Missing 'confidence'"
        assert "keywords" in c,         "Missing 'keywords'"
        assert "short_explanation" in c, "Missing 'short_explanation'"
        assert c["type"] in {"object", "process", "structure", "diagram"}, \
            f"Invalid type: {c['type']}"
        assert 0.0 <= c["confidence"] <= 1.0, \
            f"Confidence out of range: {c['confidence']}"
        assert isinstance(c["keywords"], list), "keywords must be a list"
        assert len(c["keywords"]) <= 5, "Max 5 keywords"

    print("✓ All assertions passed.")
