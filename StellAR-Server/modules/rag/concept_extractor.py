"""
Concept Extractor Module
========================
Extracts visualizable educational concepts from text chunks using a local
Ollama model and returns concept dicts compatible with the downstream image
retrieval and model-generation pipeline.
"""

from __future__ import annotations

import json
import logging
import os
import re
import uuid
from typing import Any, Dict, List, Optional

from modules.local_llm import OLLAMA_MODEL_NAME, extract_json_fragment, generate_with_ollama

MAX_CONCEPTS_PER_CHUNK = 6
MAX_CHUNK_CHARACTERS = int(os.environ.get("RAG_CONCEPT_CHUNK_CHARS", "1800"))
OLLAMA_TIMEOUT_SECONDS = int(os.environ.get("RAG_CONCEPT_TIMEOUT_SECONDS", "45"))

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """You extract visualizable educational concepts for an AR learning pipeline.

Return ONLY a valid JSON array.
Each array item must be an object with exactly these keys:
- "concept": string
- "description": string

Rules:
- Extract only important visualizable objects, structures, systems, or processes.
- Keep descriptions short and grounded in the provided text.
- Return at most 6 concepts.
- Avoid duplicates, synonyms, and overly abstract ideas.
- If nothing useful is present, return [].
- Do not include markdown, code fences, or explanatory text."""

USER_PROMPT_TEMPLATE = """Text chunk:
\"\"\"
{chunk}
\"\"\"

Extract the top visualizable concepts from this chunk.
Return JSON only."""

_STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in", "into",
    "is", "it", "of", "on", "or", "that", "the", "their", "this", "to", "with",
}

_VISUAL_TERMS = [
    "algae",
    "amino acid",
    "amoeba",
    "artery",
    "bacteria",
    "blood",
    "blood vessel",
    "carbohydrate",
    "cell",
    "cell membrane",
    "chlorophyll",
    "chloroplast",
    "circulatory system",
    "digestion",
    "digestive system",
    "dna",
    "ecosystem",
    "enzyme",
    "fat",
    "food chain",
    "heart",
    "intestine",
    "kidney",
    "leaf",
    "liver",
    "lung",
    "mitochondria",
    "mouth",
    "nutrition",
    "organ",
    "organ system",
    "organism",
    "photosynthesis",
    "plant",
    "protein",
    "reproduction",
    "respiration",
    "root",
    "stomach",
    "tissue",
    "vein",
    "villus",
]


def _split_for_ollama(chunk: str) -> List[str]:
    text = (chunk or "").strip()
    if not text:
        return []
    if len(text) <= MAX_CHUNK_CHARACTERS:
        return [text]

    parts: List[str] = []
    cursor = 0
    while cursor < len(text):
        window = text[cursor:cursor + MAX_CHUNK_CHARACTERS]
        if cursor + MAX_CHUNK_CHARACTERS < len(text):
            split_at = max(window.rfind("\n\n"), window.rfind(". "), window.rfind(" "))
            if split_at > MAX_CHUNK_CHARACTERS // 2:
                window = window[:split_at].strip()
        parts.append(window.strip())
        cursor += max(len(window), 1)

    return [part for part in parts if part]


def extract_concepts_json(text_chunk: str) -> List[Dict[str, str]]:
    """
    Input: text chunk
    Output: JSON-compatible list of {"concept", "description"} objects
    """
    if not text_chunk or not text_chunk.strip():
        return []

    prompt = USER_PROMPT_TEMPLATE.format(chunk=text_chunk.strip())

    try:
        raw_output = generate_with_ollama(
            prompt=prompt,
            system=SYSTEM_PROMPT,
            model=OLLAMA_MODEL_NAME,
            format="json",
            options={
                "temperature": 0.1,
                "num_predict": 500,
            },
            timeout=OLLAMA_TIMEOUT_SECONDS,
        )
    except Exception as exc:
        logger.warning("Ollama concept extraction failed: %s", exc)
        return _fallback_extract_concepts_json(text_chunk)

    payload = _parse_raw_concepts(raw_output)
    if not payload:
        return _fallback_extract_concepts_json(text_chunk)

    return payload[:MAX_CONCEPTS_PER_CHUNK]


def _parse_raw_concepts(raw_output: str) -> List[Dict[str, str]]:
    json_fragment = extract_json_fragment(raw_output)
    if not json_fragment:
        logger.warning("No JSON fragment found in Ollama response")
        return []

    try:
        data = json.loads(json_fragment)
    except json.JSONDecodeError as exc:
        logger.warning("Invalid concept JSON from Ollama: %s", exc)
        return []

    if isinstance(data, dict):
        for key in ("concepts", "items", "data", "results"):
            if isinstance(data.get(key), list):
                data = data[key]
                break
        else:
            data = [data] if "concept" in data else []

    if not isinstance(data, list):
        return []

    concepts: List[Dict[str, str]] = []
    seen: set[str] = set()

    for item in data:
        if not isinstance(item, dict):
            continue

        concept = str(item.get("concept", "")).strip()
        description = str(item.get("description", "")).strip()
        normalized = concept.lower()

        if not concept or not description or normalized in seen:
            continue

        seen.add(normalized)
        concepts.append({
            "concept": concept,
            "description": description,
        })

    return concepts


def _sentence_for_term(text: str, term: str) -> str:
    sentences = re.split(r"(?<=[.!?])\s+", re.sub(r"\s+", " ", text).strip())
    pattern = re.compile(rf"\b{re.escape(term)}s?\b", re.IGNORECASE)
    for sentence in sentences:
        if pattern.search(sentence):
            return sentence.strip()
    return sentences[0].strip() if sentences else ""


def _make_description(term: str, sentence: str) -> str:
    clean_sentence = sentence.strip()
    if not clean_sentence:
        return f"{term.title()} is an important visual concept from the uploaded text."
    if len(clean_sentence) > 180:
        clean_sentence = clean_sentence[:177].rsplit(" ", 1)[0].rstrip(",;:") + "..."
    return clean_sentence


def _title_from_term(term: str) -> str:
    known_lowercase = {"dna"}
    words = []
    for word in term.split():
        words.append(word.upper() if word in known_lowercase else word.capitalize())
    return " ".join(words)


def _fallback_extract_concepts_json(text_chunk: str) -> List[Dict[str, str]]:
    """
    Lightweight local backup for demos and offline runs when Ollama is unhealthy.
    It favors concrete biology terms, then simple textbook definition patterns.
    """
    text = (text_chunk or "").strip()
    if not text:
        return []

    concepts: List[Dict[str, str]] = []
    seen: set[str] = set()

    def add_concept(term: str, sentence: str) -> None:
        normalized = term.lower().strip()
        if not normalized or normalized in seen:
            return
        seen.add(normalized)
        concepts.append({
            "concept": _title_from_term(normalized),
            "description": _make_description(_title_from_term(normalized), sentence),
        })

    for term in sorted(_VISUAL_TERMS, key=len, reverse=True):
        if len(concepts) >= MAX_CONCEPTS_PER_CHUNK:
            break
        if re.search(rf"\b{re.escape(term)}s?\b", text, flags=re.IGNORECASE):
            add_concept(term, _sentence_for_term(text, term))

    definition_patterns = (
        r"\b([A-Z][A-Za-z][A-Za-z\s\-]{2,40})\s+(?:is|are|refers to|means)\s+([^.!?]{20,180})",
        r"\b(?:process of|structure of|function of)\s+([A-Za-z][A-Za-z\s\-]{2,40})",
    )
    for pattern in definition_patterns:
        if len(concepts) >= MAX_CONCEPTS_PER_CHUNK:
            break
        for match in re.finditer(pattern, text):
            if len(concepts) >= MAX_CONCEPTS_PER_CHUNK:
                break
            term = re.sub(r"\s+", " ", match.group(1)).strip(" -,:;")
            words = [word for word in term.split() if word.lower() not in _STOP_WORDS]
            if not (1 <= len(words) <= 4):
                continue
            add_concept(" ".join(words), _sentence_for_term(text, term))

    if concepts:
        logger.info("Fallback concept extraction produced %d concept(s)", len(concepts))
    return concepts[:MAX_CONCEPTS_PER_CHUNK]


def _infer_type(concept: str, description: str) -> str:
    haystack = f"{concept} {description}".lower()
    if any(term in haystack for term in ("process", "cycle", "division", "reaction", "flow", "formation")):
        return "process"
    if any(term in haystack for term in ("system", "structure", "layer", "membrane", "organ", "network")):
        return "structure"
    if any(term in haystack for term in ("diagram", "model", "map", "chart")):
        return "diagram"
    return "object"


def _build_keywords(concept: str, description: str) -> List[str]:
    words = re.findall(r"[A-Za-z][A-Za-z\-]+", f"{concept} {description}".lower())
    ordered: List[str] = []
    for word in words:
        if len(word) < 3 or word in _STOP_WORDS or word in ordered:
            continue
        ordered.append(word)
        if len(ordered) == 5:
            break
    if concept.lower() not in ordered:
        ordered = [concept.strip()] + ordered
    return ordered[:5]


def _to_pipeline_concept(item: Dict[str, str]) -> Optional[Dict[str, Any]]:
    concept = item.get("concept", "").strip()
    description = item.get("description", "").strip()
    if not concept or not description:
        return None

    return {
        "id": str(uuid.uuid4()),
        "title": concept,
        "type": _infer_type(concept, description),
        "confidence": 0.85,
        "keywords": _build_keywords(concept, description),
        "short_explanation": description,
    }


def _extract_from_chunk(chunk: str) -> List[Dict[str, Any]]:
    extracted = extract_concepts_json(chunk)
    results: List[Dict[str, Any]] = []

    for item in extracted[:MAX_CONCEPTS_PER_CHUNK]:
        concept = _to_pipeline_concept(item)
        if concept is not None:
            results.append(concept)

    return results


def extract_concepts(chunks: List[str]) -> List[Dict[str, Any]]:
    """
    Extract visualizable concepts from a list of chunks while preserving the
    existing pipeline's return shape.
    """
    if not chunks:
        logger.warning("extract_concepts called with empty chunk list")
        return []

    normalized_chunks: List[str] = []
    for chunk in chunks:
        normalized_chunks.extend(_split_for_ollama(chunk))

    all_concepts: List[Dict[str, Any]] = []
    seen_titles: set[str] = set()

    for idx, chunk in enumerate(normalized_chunks):
        logger.info(
            "Processing concept chunk %d/%d with local Ollama (%d chars)",
            idx + 1,
            len(normalized_chunks),
            len(chunk),
        )

        for concept in _extract_from_chunk(chunk):
            title_key = concept.get("title", "").strip().lower()
            if not title_key or title_key in seen_titles:
                continue
            seen_titles.add(title_key)
            all_concepts.append(concept)

    logger.info(
        "Concept extraction complete: %d concept(s) from %d original chunks",
        len(all_concepts),
        len(chunks),
    )
    return all_concepts
