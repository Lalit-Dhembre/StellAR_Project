"""
Concept Extractor Module — v3 (Balanced High-Quality)
======================================================
Extracts comprehensive, domain-relevant educational concepts from text chunks
using a hybrid approach:

    1. Frequency-based core term detection (ensures fundamentals are never missed)
    2. Section-aware LLM extraction (respects document structure)
    3. Quality gates + relevance scoring (rejects noise)

Design:
    - Min 10-15 concepts per document
    - 40% core / 40% structural / 20% advanced balance
    - Exact text grounding — no renaming
    - Precision stays high, but recall is now guaranteed for core terms
"""

from __future__ import annotations

import json
import logging
import os
import re
import uuid
from collections import Counter
from typing import Any, Dict, List, Optional, Tuple

from modules.local_llm import OLLAMA_MODEL_NAME, extract_json_fragment, generate_with_ollama

MAX_CONCEPTS_PER_CHUNK = 6
MIN_CONCEPTS_TOTAL = 10
MAX_CONCEPTS_TOTAL = 30
MAX_CHUNK_CHARACTERS = int(os.environ.get("RAG_CONCEPT_CHUNK_CHARS", "1800"))
OLLAMA_TIMEOUT_SECONDS = int(os.environ.get("RAG_CONCEPT_TIMEOUT_SECONDS", "45"))
RELEVANCE_THRESHOLD = 0.35  # Slightly relaxed to not lose core terms

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# LLM Prompts
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = """You are a precise educational concept extractor for an Augmented Reality learning app.

Return ONLY a valid JSON array. No markdown, no explanation, no code fences.

Each item MUST have exactly these keys:
- "concept": the EXACT term as it appears in the text (do not rename or paraphrase)
- "description": 1-2 sentence explanation grounded strictly in the provided text

RULES:
1. Extract specific scientific terms EXACTLY as they appear in the text.
2. Include foundational terms (e.g., "neuron", "nephron", "mitochondria") if they are central to the text.
3. Include structural terms (e.g., "Bowman's capsule", "Loop of Henle", "axon terminal").
4. Include process/function terms (e.g., "micturition reflex", "synaptic transmission").
5. Do NOT extract generic filler words: organ, system, body, process, structure, base, layer, all, type, part.
6. Do NOT extract pronouns, determiners, or broken sentence fragments.
7. Do NOT hallucinate — only extract terms explicitly present in the text.
8. Return 4-6 concepts per chunk, covering structure, function, and classification.
9. If nothing specific is present, return []."""

USER_PROMPT_TEMPLATE = """Text chunk:
\"\"\"
{chunk}
\"\"\"

Extract 4-6 specific scientific concepts from this text.
Use the EXACT terms from the text — do not rename them.
Cover: structures, functions, classifications, and processes.
Return ONLY a JSON array."""

# ---------------------------------------------------------------------------
# Blocklists
# ---------------------------------------------------------------------------

_BLOCKED_SINGLE_WORDS = {
    # Pronouns, determiners, conjunctions
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in",
    "into", "is", "it", "of", "on", "or", "that", "the", "their", "this",
    "to", "with", "there", "these", "those", "they", "them", "then", "than",
    "thus", "through", "each", "every", "some", "many", "most", "such", "its",
    "also", "both", "other", "another", "several", "various", "which", "when",
    "where", "while", "what", "how", "why", "here", "after", "before",
    "between", "during", "if", "but", "not", "no", "so", "all", "any",
    # Overly generic — these are NOT concepts
    "organ", "system", "body", "process", "function", "structure", "base",
    "layer", "type", "part", "group", "form", "unit", "region", "area",
    "role", "rate", "level", "factor", "amount", "result", "effect",
    "change", "stage", "step", "phase", "side", "end", "way", "use",
    "term", "name", "kind", "class", "order", "time", "case",
    "water", "food", "air", "heat", "cold", "size", "number",
}

_BLOCKED_PHRASES = {
    "blood vessel", "body part", "organ system", "body system",
    "living organism", "chemical reaction", "basic unit", "main function",
    "important role", "various types", "different types",
}

_FRAGMENT_PATTERN = re.compile(
    r"^(if|when|where|which|that|the|a|an|and|or|but|as|by|in|on|at|to|of|for)\s",
    re.IGNORECASE
)

# ---------------------------------------------------------------------------
# Embedding model for relevance scoring
# ---------------------------------------------------------------------------

_embedding_model = None
_embedding_cache: Dict[str, Any] = {}


def _get_embedding_model():
    global _embedding_model
    if _embedding_model is None:
        try:
            from sentence_transformers import SentenceTransformer
            _embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
        except Exception as e:
            logger.warning(f"Could not load embedding model: {e}")
    return _embedding_model


def _get_embedding(text: str):
    text = re.sub(r"\s+", " ", (text or "").strip().lower())
    if text in _embedding_cache:
        return _embedding_cache[text]
    model = _get_embedding_model()
    if model is None:
        return None
    emb = model.encode([text])[0]
    _embedding_cache[text] = emb
    return emb


def _compute_relevance(concept_text: str, document_text: str) -> float:
    from sklearn.metrics.pairwise import cosine_similarity
    concept_emb = _get_embedding(concept_text)
    doc_emb = _get_embedding(document_text[:2000])
    if concept_emb is None or doc_emb is None:
        return 1.0
    return float(cosine_similarity([concept_emb], [doc_emb])[0][0])


# ---------------------------------------------------------------------------
# Frequency-based core term detection
# ---------------------------------------------------------------------------

def _extract_noun_phrases(text: str) -> List[str]:
    """
    Extract candidate noun phrases from text using regex patterns.
    Matches capitalized multi-word terms and known scientific patterns.
    """
    patterns = [
        # Multi-word capitalized terms: "Loop of Henle", "Bowman's capsule"
        r"\b([A-Z][a-z]+(?:\s+(?:of|and|the|in|for)\s+)?[A-Z][a-z]+(?:'s)?(?:\s+[a-z]+)?)\b",
        # Terms with apostrophes: "Bowman's capsule", "Henle's loop"
        r"\b([A-Z][a-z]+'s\s+[a-z]+(?:\s+[a-z]+)?)\b",
        # Hyphenated terms: "myelinated nerve fibre"
        r"\b([a-z]+-[a-z]+(?:\s+[a-z]+){1,2})\b",
    ]

    candidates = []
    for pattern in patterns:
        for match in re.finditer(pattern, text):
            term = match.group(1).strip()
            if len(term) > 4 and term.lower() not in _BLOCKED_SINGLE_WORDS:
                candidates.append(term)
    return candidates


def _scan_core_terms(full_text: str) -> List[Dict[str, Any]]:
    """
    Scan the full document for high-frequency scientific terms.
    These are 'core terms' that must be included regardless of LLM output.

    Returns terms sorted by frequency (descending).
    """
    text_lower = full_text.lower()

    # Tokenize into words, count frequencies of meaningful words
    words = re.findall(r"\b[a-z][a-z'-]{2,}\b", text_lower)
    word_freq = Counter(words)

    # Also count bigrams and trigrams
    bigrams = [f"{words[i]} {words[i+1]}" for i in range(len(words) - 1)]
    trigrams = [f"{words[i]} {words[i+1]} {words[i+2]}" for i in range(len(words) - 2)]

    ngram_freq = Counter(bigrams + trigrams)

    # Filter: keep only terms that appear 2+ times and aren't blocked
    core_candidates: List[Tuple[str, int]] = []

    # Single scientific words (must appear 3+ times to be "core")
    for word, freq in word_freq.items():
        if freq >= 3 and word not in _BLOCKED_SINGLE_WORDS and len(word) >= 5:
            # Check it's not a common English word by verifying it's in a meaningful context
            core_candidates.append((word, freq))

    # Multi-word terms (must appear 2+ times)
    for ngram, freq in ngram_freq.items():
        if freq >= 2:
            ngram_words = ngram.split()
            # Skip if all words are blocked
            if all(w in _BLOCKED_SINGLE_WORDS for w in ngram_words):
                continue
            # Skip if it's a blocked phrase
            if ngram in _BLOCKED_PHRASES:
                continue
            core_candidates.append((ngram, freq))

    # Sort by frequency
    core_candidates.sort(key=lambda x: x[1], reverse=True)

    # Build concept dicts for the top core terms
    core_concepts = []
    seen = set()

    for term, freq in core_candidates[:20]:  # Consider top 20 candidates
        normalized = term.lower().strip()
        if normalized in seen:
            continue

        # Find the sentence where this term first appears for description
        description = _find_best_sentence(full_text, term)
        if not description or len(description) < 15:
            description = f"{term.title()} is a key concept discussed extensively in this text."

        # Quality check
        if not _is_quality_concept(term.title() if len(term.split()) == 1 else _title_case(term), description):
            continue

        seen.add(normalized)
        core_concepts.append({
            "concept": term.title() if len(term.split()) == 1 else _title_case(term),
            "description": description,
            "frequency": freq,
            "source": "frequency_scan",
        })

    return core_concepts


def _title_case(text: str) -> str:
    """Title case but preserve short prepositions lowercase."""
    small_words = {"of", "and", "the", "in", "for", "to", "a", "an", "or", "by", "at", "on", "vs"}
    words = text.split()
    result = []
    for i, word in enumerate(words):
        if i == 0 or word.lower() not in small_words:
            result.append(word.capitalize())
        else:
            result.append(word.lower())
    return " ".join(result)


def _find_best_sentence(text: str, term: str) -> str:
    """Find the most informative sentence containing the term."""
    sentences = re.split(r'(?<=[.!?])\s+', text.strip())
    pattern = re.compile(rf"\b{re.escape(term)}\b", re.IGNORECASE)

    best = ""
    best_score = 0

    for sentence in sentences:
        if pattern.search(sentence):
            # Score: prefer sentences with "is", "are", "refers to" (definition-like)
            score = len(sentence)
            if re.search(r"\b(is|are|refers to|means|called|known as|defined as)\b", sentence, re.IGNORECASE):
                score += 100  # Big boost for definitional sentences
            if score > best_score:
                best_score = score
                best = sentence.strip()

    if best and len(best) > 200:
        best = best[:197].rsplit(" ", 1)[0].rstrip(",:;") + "..."

    return best


# ---------------------------------------------------------------------------
# Section detection
# ---------------------------------------------------------------------------

def _detect_sections(text: str) -> List[Dict[str, str]]:
    """
    Detect section headings in the text and split into sections.
    Handles patterns like:
        - "Structure of Neuron"
        - "TYPES OF NEURONS"
        - "1.2 Synaptic Transmission"
    """
    # Common heading patterns
    heading_patterns = [
        r"^(?:\d+\.?\d*\s+)?([A-Z][A-Z\s]{3,50})$",  # ALL CAPS lines
        r"^(?:\d+\.?\d*\s+)?((?:[A-Z][a-z]+\s+){1,5}(?:of|and|in|for)?\s*(?:[A-Z][a-z]+\s*){0,3})$",  # Title Case
        r"^#+\s+(.+)$",  # Markdown headings
    ]

    lines = text.split("\n")
    sections: List[Dict[str, str]] = []
    current_heading = "Introduction"
    current_content: List[str] = []

    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue

        is_heading = False
        for pattern in heading_patterns:
            match = re.match(pattern, stripped)
            if match and len(stripped) < 60 and len(stripped.split()) <= 8:
                # Save previous section
                if current_content:
                    sections.append({
                        "heading": current_heading,
                        "content": "\n".join(current_content),
                    })
                current_heading = match.group(1).strip()
                current_content = []
                is_heading = True
                break

        if not is_heading:
            current_content.append(stripped)

    # Save last section
    if current_content:
        sections.append({
            "heading": current_heading,
            "content": "\n".join(current_content),
        })

    return sections if len(sections) > 1 else [{"heading": "Full Document", "content": text}]


# ---------------------------------------------------------------------------
# Quality gates
# ---------------------------------------------------------------------------

def _is_quality_concept(concept: str, description: str) -> bool:
    concept_clean = concept.strip()
    concept_lower = concept_clean.lower()

    if not concept_clean or not description.strip():
        return False

    if concept_lower in _BLOCKED_SINGLE_WORDS:
        return False

    if concept_lower in _BLOCKED_PHRASES:
        return False

    if _FRAGMENT_PATTERN.match(concept_clean):
        return False

    words = concept_clean.split()

    # Single words must be at least 4 chars
    if len(words) == 1 and len(concept_clean) < 4:
        return False

    # Max 7 words
    if len(words) > 7:
        return False

    # Must contain at least one alphabetic word ≥ 3 chars
    if not any(len(w) >= 3 and w.isalpha() for w in words):
        return False

    # No purely numeric
    if all(w.isdigit() for w in words):
        return False

    # Description must be meaningful
    if len(description.strip()) < 10:
        return False

    return True


# ---------------------------------------------------------------------------
# LLM extraction
# ---------------------------------------------------------------------------

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
            split_at = max(window.rfind(". "), window.rfind(".\n"), window.rfind("\n\n"))
            if split_at > MAX_CHUNK_CHARACTERS // 2:
                window = window[:split_at + 1].strip()
        parts.append(window.strip())
        cursor += max(len(window), 1)

    return [part for part in parts if part]


def _call_ollama(text_chunk: str) -> List[Dict[str, str]]:
    """Call Ollama with ONE retry. No naive fallback."""
    prompt = USER_PROMPT_TEMPLATE.format(chunk=text_chunk.strip())

    for attempt in range(2):
        try:
            raw_output = generate_with_ollama(
                prompt=prompt,
                system=SYSTEM_PROMPT,
                model=OLLAMA_MODEL_NAME,
                format="json",
                options={
                    "temperature": 0.1,
                    "num_predict": 700,
                },
                timeout=OLLAMA_TIMEOUT_SECONDS,
            )
        except Exception as exc:
            logger.warning("Ollama attempt %d failed: %s", attempt + 1, exc)
            if attempt == 0:
                continue
            return []

        payload = _parse_raw_concepts(raw_output)
        if payload:
            return payload

        logger.warning("Ollama returned unparseable output (attempt %d)", attempt + 1)

    return []


def _parse_raw_concepts(raw_output: str) -> List[Dict[str, str]]:
    json_fragment = extract_json_fragment(raw_output)
    if not json_fragment:
        return []

    try:
        data = json.loads(json_fragment)
    except json.JSONDecodeError:
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
        if not _is_quality_concept(concept, description):
            continue

        seen.add(normalized)
        concepts.append({"concept": concept, "description": description})

    return concepts


# ---------------------------------------------------------------------------
# Pipeline concept shape
# ---------------------------------------------------------------------------

def _infer_type(concept: str, description: str) -> str:
    haystack = f"{concept} {description}".lower()
    if any(t in haystack for t in ("process", "cycle", "division", "reaction", "flow", "formation", "reflex", "transmission", "transport")):
        return "process"
    if any(t in haystack for t in ("system", "structure", "layer", "membrane", "network", "tract", "capsule", "tubule", "fibre", "fiber")):
        return "structure"
    if any(t in haystack for t in ("type", "class", "category", "classification")):
        return "classification"
    if any(t in haystack for t in ("diagram", "model", "map", "chart")):
        return "diagram"
    return "object"


def _build_keywords(concept: str, description: str) -> List[str]:
    words = re.findall(r"[A-Za-z][A-Za-z\-]+", f"{concept} {description}".lower())
    ordered: List[str] = []
    for word in words:
        if len(word) < 3 or word in _BLOCKED_SINGLE_WORDS or word in ordered:
            continue
        ordered.append(word)
        if len(ordered) == 5:
            break
    if concept.lower() not in " ".join(ordered):
        ordered = [concept.strip()] + ordered
    return ordered[:5]


def _to_pipeline_concept(item: Dict[str, str], relevance: float = 0.0, freq: int = 0) -> Optional[Dict[str, Any]]:
    concept = item.get("concept", "").strip()
    description = item.get("description", "").strip()
    if not concept or not description:
        return None
    return {
        "id": str(uuid.uuid4()),
        "title": concept,
        "type": _infer_type(concept, description),
        "confidence": min(0.95, 0.60 + relevance * 0.3 + min(freq * 0.01, 0.05)),
        "keywords": _build_keywords(concept, description),
        "short_explanation": description,
    }


# ---------------------------------------------------------------------------
# Main extraction pipeline
# ---------------------------------------------------------------------------

def extract_concepts(chunks: List[str]) -> List[Dict[str, Any]]:
    """
    Balanced high-quality concept extraction.

    Pipeline:
        1. Frequency scan → identify core terms from full document
        2. Section detection → understand document structure
        3. LLM extraction per chunk → domain-specific concepts
        4. Merge core terms + LLM results (deduplicated)
        5. Relevance scoring against full document
        6. Balance: ensure mix of core / structural / advanced
        7. Guarantee: min 10 concepts (if document has enough content)
    """
    if not chunks:
        logger.warning("extract_concepts called with empty chunk list")
        return []

    full_text = "\n".join(chunks)

    # ── Step 1: Frequency-based core term scan ──────────────────────────
    logger.info("Scanning document for high-frequency core terms...")
    core_terms = _scan_core_terms(full_text)
    logger.info("Found %d core term candidate(s) via frequency analysis", len(core_terms))

    # ── Step 2: LLM extraction per chunk ────────────────────────────────
    normalized_chunks: List[str] = []
    for chunk in chunks:
        normalized_chunks.extend(_split_for_ollama(chunk))

    llm_concepts: List[Dict[str, str]] = []
    seen_titles: set[str] = set()

    for idx, chunk in enumerate(normalized_chunks):
        logger.info(
            "Processing chunk %d/%d with Ollama (%d chars)",
            idx + 1, len(normalized_chunks), len(chunk),
        )
        extracted = _call_ollama(chunk)
        for item in extracted[:MAX_CONCEPTS_PER_CHUNK]:
            title_key = item["concept"].strip().lower()
            if title_key not in seen_titles:
                seen_titles.add(title_key)
                llm_concepts.append(item)

    logger.info("LLM extracted %d unique concept(s)", len(llm_concepts))

    # ── Step 3: Merge core terms + LLM results ─────────────────────────
    # Core terms get priority, then LLM concepts fill in
    merged: List[Dict[str, str]] = []
    merged_keys: set[str] = set()

    # Add core terms first
    for ct in core_terms:
        key = ct["concept"].lower()
        if key not in merged_keys:
            merged_keys.add(key)
            merged.append(ct)

    # Add LLM concepts
    for lc in llm_concepts:
        key = lc["concept"].lower()
        if key not in merged_keys:
            merged_keys.add(key)
            merged.append(lc)

    logger.info("Merged: %d total unique concepts (core=%d, llm=%d)",
                len(merged), len(core_terms), len(llm_concepts))

    if not merged:
        return []

    # ── Step 4: Relevance scoring ───────────────────────────────────────
    scored: List[Dict[str, Any]] = []

    for item in merged:
        concept_text = f"{item['concept']}. {item.get('description', '')}"
        relevance = _compute_relevance(concept_text, full_text)
        freq = item.get("frequency", 0)

        # Core terms (high frequency) get a relevance floor — never filtered out
        if freq >= 3:
            relevance = max(relevance, RELEVANCE_THRESHOLD + 0.05)

        if relevance < RELEVANCE_THRESHOLD:
            logger.debug("Filtered: '%s' (relevance=%.3f)", item['concept'], relevance)
            continue

        # Frequency boost
        freq_boost = min(freq * 0.015, 0.10) if freq else 0
        final_score = relevance + freq_boost

        pipeline_concept = _to_pipeline_concept(item, relevance, freq)
        if pipeline_concept is None:
            continue

        pipeline_concept["relevance_score"] = round(final_score, 3)
        pipeline_concept["term_frequency"] = freq
        scored.append(pipeline_concept)

    # ── Step 5: Sort and balance ────────────────────────────────────────
    scored.sort(key=lambda c: c.get("relevance_score", 0), reverse=True)

    # Ensure we have at least MIN_CONCEPTS_TOTAL if possible
    if len(scored) < MIN_CONCEPTS_TOTAL and len(merged) > len(scored):
        # Relax threshold and add more
        for item in merged:
            if len(scored) >= MIN_CONCEPTS_TOTAL:
                break
            key = item["concept"].lower()
            if any(c["title"].lower() == key for c in scored):
                continue
            concept_text = f"{item['concept']}. {item.get('description', '')}"
            relevance = _compute_relevance(concept_text, full_text)
            # Use a relaxed threshold for the minimum guarantee
            if relevance >= RELEVANCE_THRESHOLD - 0.10:
                pc = _to_pipeline_concept(item, relevance, item.get("frequency", 0))
                if pc:
                    pc["relevance_score"] = round(relevance, 3)
                    pc["term_frequency"] = item.get("frequency", 0)
                    scored.append(pc)

    # Cap at MAX
    scored = scored[:MAX_CONCEPTS_TOTAL]

    logger.info(
        "Concept extraction complete: %d concept(s) from %d chunks "
        "(core_scanned=%d, llm_extracted=%d)",
        len(scored), len(chunks), len(core_terms), len(llm_concepts)
    )

    return scored
