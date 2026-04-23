"""
Concept Extractor Module
========================
Extracts visualizable educational concepts from text chunks using a local
Ollama model and returns concept dicts compatible with the downstream image
retrieval and model-generation pipeline.

Improvements over v1:
- 2–5 concepts per chunk (structured: 1 main + 2–4 supporting)
- Spell correction for common OCR/student typos
- Rejects invalid concepts (properties, partial phrases)
- Fallback covers anatomy, histology, classification terms
- Minimum 10–15 concepts per document target
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

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

MAX_CONCEPTS_PER_CHUNK = 5        # Raised from 6 (no change) but enforced at 5 per call
MIN_CONCEPTS_PER_CHUNK = 2        # Always try to get at least 2
TARGET_CONCEPTS_PER_DOC = 12      # Target for the entire document
MAX_CHUNK_CHARACTERS    = int(os.environ.get("RAG_CONCEPT_CHUNK_CHARS", "1800"))
OLLAMA_TIMEOUT_SECONDS  = int(os.environ.get("RAG_CONCEPT_TIMEOUT_SECONDS", "60"))

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Spell correction dictionary  (common OCR / student typo → correct form)
# ---------------------------------------------------------------------------

_SPELL_CORRECTIONS: Dict[str, str] = {
    # Histology / Epithelium
    "globlet":          "goblet",
    "gobelt":           "goblet",
    "ephithelium":      "epithelium",
    "epethelium":       "epithelium",
    "epitheliun":       "epithelium",
    "squameous":        "squamous",
    "squamaus":         "squamous",
    "collumar":         "columnar",
    "columar":          "columnar",
    "cuboidal":         "cuboidal",
    "cubodial":         "cuboidal",
    "basment":          "basement",
    "basemant":         "basement",
    "microvily":        "microvilli",
    "microvillie":      "microvilli",
    "cila":             "cilia",
    "cillia":           "cilia",
    "flagela":          "flagella",
    "flagella":         "flagella",
    "secretoin":        "secretion",
    "absorbtion":       "absorption",
    "absorbsion":       "absorption",
    # Cell biology
    "mitocondria":      "mitochondria",
    "mitochondrea":     "mitochondria",
    "nuclues":          "nucleus",
    "nuclius":          "nucleus",
    "cytoplasim":       "cytoplasm",
    "ribosome":         "ribosome",
    "ribsome":          "ribosome",
    "chromosone":       "chromosome",
    "chormosome":       "chromosome",
    "vacuole":          "vacuole",
    "vacuol":           "vacuole",
    "lysosome":         "lysosome",
    "lysosme":          "lysosome",
    "endoplasmic reticulem": "endoplasmic reticulum",
    "golgi apperatus":  "golgi apparatus",
    "golgi aparatus":   "golgi apparatus",
    # Plant biology
    "clorophyll":       "chlorophyll",
    "chlorofil":        "chlorophyll",
    "chloroplats":      "chloroplasts",
    "photosynethsis":   "photosynthesis",
    "photosythesis":    "photosynthesis",
    "stomatta":         "stomata",
    "stomatta":         "stomata",
    # General biology
    "difusion":         "diffusion",
    "osmossis":         "osmosis",
    "meiosos":          "meiosis",
    "mitossis":         "mitosis",
    "enzime":           "enzyme",
    "protien":          "protein",
    "glucouse":         "glucose",
    "haemogloben":      "haemoglobin",
    "haemoglobim":      "haemoglobin",
    # Physics
    "velosity":         "velocity",
    "accelaration":     "acceleration",
    "fricton":          "friction",
    "magnitism":        "magnetism",
    "momentem":         "momentum",
    "fource":           "force",
    "electon":          "electron",
    "lense":            "lens",
    # Space / Astronomy
    "astroid":          "asteroid",
    "meteroid":         "meteoroid",
    "galexy":           "galaxy",
    "satelite":         "satellite",
    "solor":            "solar",
    "orbet":            "orbit",
    "planetery":        "planetary",
    "meteorite":        "meteorite",
    # History
    "civilzation":      "civilization",
    "empeir":           "empire",
    "pharoah":          "pharaoh",
    "revolusion":       "revolution",
    "dynesty":          "dynasty",
    "architecure":      "architecture",
    "artifcat":         "artifact",
}


def _spell_correct(text: str) -> str:
    """Apply word-level spell corrections to extracted concept names."""
    lower = text.lower().strip()
    if lower in _SPELL_CORRECTIONS:
        corrected = _SPELL_CORRECTIONS[lower]
        if text and text[0].isupper():
            return corrected.title()
        return corrected
    # Scan inside compound terms word by word
    words = text.split()
    corrected_words = []
    for w in words:
        wl = w.lower()
        replacement = _SPELL_CORRECTIONS.get(wl)
        if replacement:
            corrected_words.append(replacement.capitalize() if w[0].isupper() else replacement)
        else:
            corrected_words.append(w)
    return " ".join(corrected_words)


# ---------------------------------------------------------------------------
# Concepts that should always be rejected (properties, not objects/structures)
# ---------------------------------------------------------------------------

_INVALID_CONCEPT_PATTERNS = [
    re.compile(r"^\s*(single|double|multiple|many|few|large|small|thin|thick|flat|round)\s+", re.I),
    re.compile(r"\bnucleus\s+(is|are|has)\b", re.I),       # "single nucleus" type phrase
    re.compile(r"^[a-z][a-z]+\s+[a-z]"),                   # lowercase multi-word = likely partial phrase
    re.compile(r"\d"),                                       # contains digits — likely a property
    # NOTE: -tion/-ing/-ment NOT rejected: absorption, secretion, digestion are valid processes
]

_INVALID_EXACT = {
    "cell", "cells", "structure", "function", "type", "types",
    "layer", "form", "shape", "size", "characteristic", "property",
    "feature", "example", "classification", "category",
}


def _is_valid_concept(name: str) -> bool:
    """Return True if the concept name is a real biological entity, not a property/fragment."""
    name = name.strip()
    if not name or len(name) < 3:
        return False
    if name.lower() in _INVALID_EXACT:
        return False
    for pat in _INVALID_CONCEPT_PATTERNS:
        if pat.search(name):
            return False
    # Reject if it's just stop words
    words = [w for w in name.lower().split() if w not in _STOP_WORDS]
    if not words:
        return False
    return True


# ---------------------------------------------------------------------------
# Prompts — structured 1 main + 2–4 supporting concepts
# ---------------------------------------------------------------------------

SYSTEM_PROMPT_TEMPLATE = """\
You are an expert {domain} teacher extracting educational concepts for an AR \
learning app. Your goal is COMPREHENSIVE COVERAGE — extract every important \
concept, structure, type, and process from the text.

Return ONLY a valid JSON object with exactly this structure:
{{
  "main_concept": {{"concept": "...", "description": "..."}},
  "supporting_concepts": [
    {{"concept": "...", "description": "..."}},
    {{"concept": "...", "description": "..."}}
  ]
}}

Rules:
- main_concept: the single most important visualizable object, structure, or entity.
- supporting_concepts: 2 to 4 additional important concepts from the text.
- Include: objects, structures, entities, layers, mechanisms, processes, \
classifications relevant to {domain}.
- descriptions: 1 clear sentence grounded in the text.
- Spell all terms correctly.
- DO NOT include: vague properties ("large size"), partial phrases, \
abstract ideas without a physical form.
- DO NOT include markdown, code fences, or explanation outside the JSON."""

USER_PROMPT_TEMPLATE = """\
Text chunk:
\"\"\"
{chunk}
\"\"\"

Extract the main concept and 2–4 supporting concepts.
Ensure complete coverage of all types, structures, and classifications mentioned.
Return JSON only."""


# ---------------------------------------------------------------------------
# Stop words (for fallback keyword extraction)
# ---------------------------------------------------------------------------

_STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "in",
    "into", "is", "it", "of", "on", "or", "that", "the", "their", "this",
    "to", "with", "there", "these", "those", "they", "them", "then", "than",
    "thus", "through", "each", "every", "some", "many", "most", "such",
    "its", "also", "both", "other", "another", "several", "various", "which",
    "when", "where", "while", "what", "how", "why", "here", "after",
    "before", "between", "during", "has", "have", "had", "was", "were",
    "can", "may", "will", "would", "could", "should",
}

# ---------------------------------------------------------------------------
# Expanded fallback vocabulary — covers biology, physics, space, history
# ---------------------------------------------------------------------------

_VISUAL_TERMS_BY_DOMAIN = {
    "biology": [
        "simple squamous epithelium",
        "simple cuboidal epithelium",
        "simple columnar epithelium",
        "stratified squamous epithelium",
        "stratified cuboidal epithelium",
        "stratified columnar epithelium",
        "pseudostratified columnar epithelium",
        "transitional epithelium",
        "compound epithelium",
        "simple epithelium",
        "squamous epithelium",
        "cuboidal epithelium",
        "columnar epithelium",
        "epithelium",
        "epithelial tissue",
        "basement membrane",
        "goblet cell",
        "microvilli",
        "cilia",
        "flagella",
        "junctional complex",
        "tight junction",
        "gap junction",
        "desmosome",
        "connective tissue",
        "areolar tissue",
        "adipose tissue",
        "dense regular tissue",
        "dense irregular tissue",
        "hyaline cartilage",
        "elastic cartilage",
        "fibrocartilage",
        "bone tissue",
        "blood",
        "collagen fibre",
        "elastic fibre",
        "reticular fibre",
        "fibroblast",
        "mast cell",
        "plasma cell",
        "macrophage",
        "skeletal muscle",
        "smooth muscle",
        "cardiac muscle",
        "muscle fibre",
        "sarcomere",
        "myofibril",
        "actin",
        "myosin",
        "neuron",
        "axon",
        "dendrite",
        "myelin sheath",
        "schwann cell",
        "synapse",
        "neuroglia",
        "heart",
        "liver",
        "kidney",
        "lung",
        "stomach",
        "intestine",
        "small intestine",
        "large intestine",
        "pancreas",
        "spleen",
        "brain",
        "spinal cord",
        "artery",
        "vein",
        "capillary",
        "lymph node",
        "nucleus",
        "mitochondria",
        "chloroplast",
        "ribosome",
        "endoplasmic reticulum",
        "golgi apparatus",
        "lysosome",
        "vacuole",
        "cell membrane",
        "cell wall",
        "cytoplasm",
        "cytoskeleton",
        "centriole",
        "chromosome",
        "photosynthesis",
        "respiration",
        "digestion",
        "absorption",
        "secretion",
        "excretion",
        "reproduction",
        "mitosis",
        "meiosis",
        "osmosis",
        "diffusion",
        "active transport",
        "algae",
        "bacteria",
        "virus",
        "fungi",
        "tissue",
        "organ",
        "organ system",
        "organism",
        "ecosystem",
        "food chain",
        "enzyme",
        "hormone",
        "protein",
        "carbohydrate",
        "lipid",
        "amino acid",
        "dna",
        "rna",
        "gene",
        "chromosome",
        "haemoglobin",
        "antibody",
        "antigen",
    ],
    "physics": [
        "pendulum", "pulley", "lever", "magnet", "prism", "lens", "circuit", 
        "battery", "resistor", "capacitor", "motor", "generator", "spring", 
        "telescope", "microscope", "atom", "electron", "proton", "neutron", "molecule",
        "force", "velocity", "acceleration", "friction", "gravity", "momentum"
    ],
    "space": [
        "planet", "star", "galaxy", "black hole", "nebula", "comet", "asteroid", 
        "meteor", "satellite", "moon", "solar system", "telescope", "spacecraft", 
        "rocket", "orbit", "sun", "earth", "mars", "jupiter"
    ],
    "history": [
        "pyramid", "castle", "temple", "sword", "shield", "armor", "chariot", 
        "monument", "statue", "ruins", "artifact", "coin", "map", "crown", 
        "throne", "colosseum", "ship", "civilization", "empire", "dynasty"
    ]
}


# ---------------------------------------------------------------------------
# Splitting oversized chunks
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


# ---------------------------------------------------------------------------
# LLM extraction
# ---------------------------------------------------------------------------

def extract_concepts_json(text_chunk: str, domain: str = "biology") -> List[Dict[str, str]]:
    """
    Call Ollama to extract 1 main + 2–4 supporting concepts from a text chunk.
    Returns a flat list of {"concept", "description"} dicts.
    """
    if not text_chunk or not text_chunk.strip():
        return []

    prompt = USER_PROMPT_TEMPLATE.format(chunk=text_chunk.strip())
    system_prompt = SYSTEM_PROMPT_TEMPLATE.format(domain=domain)

    try:
        raw_output = generate_with_ollama(
            prompt=prompt,
            system=system_prompt,
            model=OLLAMA_MODEL_NAME,
            format="json",
            options={
                "temperature": 0.15,
                "num_predict": 800,   # Raised — need room for 5 concepts
            },
            timeout=OLLAMA_TIMEOUT_SECONDS,
        )
    except Exception as exc:
        logger.warning("Ollama concept extraction failed: %s", exc)
        return _fallback_extract_concepts_json(text_chunk, domain)

    payload = _parse_structured_concepts(raw_output)
    if len(payload) < MIN_CONCEPTS_PER_CHUNK:
        # Supplement with fallback if LLM gave too few
        fallback = _fallback_extract_concepts_json(text_chunk, domain)
        seen = {c["concept"].lower() for c in payload}
        for fb in fallback:
            if fb["concept"].lower() not in seen:
                payload.append(fb)
                seen.add(fb["concept"].lower())
            if len(payload) >= MAX_CONCEPTS_PER_CHUNK:
                break

    return payload[:MAX_CONCEPTS_PER_CHUNK]


def _parse_structured_concepts(raw_output: str) -> List[Dict[str, str]]:
    """
    Parse the structured {"main_concept": ..., "supporting_concepts": [...]} response.
    Also handles flat arrays for backward-compatibility.
    """
    json_fragment = extract_json_fragment(raw_output)
    if not json_fragment:
        return []

    try:
        data = json.loads(json_fragment)
    except json.JSONDecodeError:
        return []

    results: List[Dict[str, str]] = []
    seen: set[str] = set()

    def _add(item: Any) -> None:
        if not isinstance(item, dict):
            return
        concept = _spell_correct(str(item.get("concept", "")).strip())
        description = str(item.get("description", "")).strip()
        key = concept.lower()
        if not concept or not description or key in seen:
            return
        if not _is_valid_concept(concept):
            logger.debug("Rejected invalid concept: %r", concept)
            return
        seen.add(key)
        results.append({"concept": concept, "description": description})

    # Structured format: {main_concept, supporting_concepts}
    if isinstance(data, dict):
        _add(data.get("main_concept"))
        for item in data.get("supporting_concepts", []):
            _add(item)
        # Fallback: flat dict with concept key
        if not results and "concept" in data:
            _add(data)
        # Fallback: wrapped list
        if not results:
            for key in ("concepts", "items", "data", "results"):
                if isinstance(data.get(key), list):
                    for item in data[key]:
                        _add(item)
                    break

    # Flat array format
    elif isinstance(data, list):
        for item in data:
            _add(item)

    return results


# ---------------------------------------------------------------------------
# Fallback: regex + vocabulary scan (no LLM required)
# ---------------------------------------------------------------------------

def _sentence_for_term(text: str, term: str) -> str:
    sentences = re.split(r"(?<=[.!?])\s+", re.sub(r"\s+", " ", text).strip())
    pattern = re.compile(rf"\b{re.escape(term)}s?\b", re.IGNORECASE)
    for sentence in sentences:
        if pattern.search(sentence):
            return sentence.strip()
    return sentences[0].strip() if sentences else ""


def _make_description(term: str, sentence: str) -> str:
    clean = sentence.strip()
    if not clean:
        return f"{term} is an important biological concept."
    if len(clean) > 200:
        clean = clean[:197].rsplit(" ", 1)[0].rstrip(",;:") + "..."
    return clean


def _title_from_term(term: str) -> str:
    known_acronyms = {"dna", "rna", "atp"}
    words = []
    for word in term.split():
        words.append(word.upper() if word in known_acronyms else word.capitalize())
    return " ".join(words)


def _fallback_extract_concepts_json(text_chunk: str, domain: str = "biology") -> List[Dict[str, str]]:
    """
    Expanded fallback: scans for vocabulary when Ollama fails.
    Targets up to MAX_CONCEPTS_PER_CHUNK concepts.
    """
    text = (text_chunk or "").strip()
    if not text:
        return []

    concepts: List[Dict[str, str]] = []
    seen: set[str] = set()

    def add_concept(term: str, sentence: str) -> None:
        corrected = _spell_correct(term)
        normalized = corrected.lower().strip()
        if not normalized or normalized in seen:
            return
        if not _is_valid_concept(corrected):
            return
        seen.add(normalized)
        concepts.append({
            "concept": _title_from_term(normalized),
            "description": _make_description(_title_from_term(normalized), sentence),
        })

    # Normalize domain to match dict keys
    dom_key = "biology"
    domain_lower = domain.lower()
    for key in _VISUAL_TERMS_BY_DOMAIN:
        if key in domain_lower:
            dom_key = key
            break

    vocab = _VISUAL_TERMS_BY_DOMAIN.get(dom_key, _VISUAL_TERMS_BY_DOMAIN["biology"])

    # Scan vocabulary longest-first to prefer multi-word matches
    for term in sorted(vocab, key=len, reverse=True):
        if len(concepts) >= MAX_CONCEPTS_PER_CHUNK:
            break
        if re.search(rf"\b{re.escape(term)}s?\b", text, flags=re.IGNORECASE):
            add_concept(term, _sentence_for_term(text, term))

    # Regex for definition-style sentences: "X is a ..." or "X are ..."
    definition_patterns = (
        r"\b([A-Z][A-Za-z][A-Za-z\s\-]{2,40})\s+(?:is|are|refers to|means)\s+([^.!?]{20,200})",
        r"\b(?:types? of|structure of|function of|forms? of)\s+([A-Za-z][A-Za-z\s\-]{2,40})",
        r"\b([A-Z][A-Za-z]{3,}(?:\s+[A-Za-z]{3,}){0,3})\s*[-–]\s*([^.!?]{15,150})",
    )
    for pattern in definition_patterns:
        if len(concepts) >= MAX_CONCEPTS_PER_CHUNK:
            break
        for match in re.finditer(pattern, text):
            if len(concepts) >= MAX_CONCEPTS_PER_CHUNK:
                break
            term = re.sub(r"\s+", " ", match.group(1)).strip(" -,:;")
            words = [w for w in term.split() if w.lower() not in _STOP_WORDS]
            if not (1 <= len(words) <= 5):
                continue
            add_concept(" ".join(words), _sentence_for_term(text, term))

    if concepts:
        logger.info("Fallback concept extraction produced %d concept(s)", len(concepts))
    return concepts[:MAX_CONCEPTS_PER_CHUNK]


# ---------------------------------------------------------------------------
# Pipeline shape conversion
# ---------------------------------------------------------------------------

def _infer_type(concept: str, description: str) -> str:
    haystack = f"{concept} {description}".lower()
    if any(t in haystack for t in ("process", "cycle", "division", "reaction",
                                    "flow", "formation", "synthesis", "transport")):
        return "process"
    if any(t in haystack for t in ("system", "structure", "layer", "membrane",
                                    "organ", "network", "junction", "matrix")):
        return "structure"
    if any(t in haystack for t in ("diagram", "model", "map", "chart")):
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
    if concept.lower() not in " ".join(ordered):
        ordered = [concept.strip()] + ordered
    return ordered[:5]


def _to_pipeline_concept(item: Dict[str, str]) -> Optional[Dict[str, Any]]:
    concept = _spell_correct(item.get("concept", "").strip())
    description = item.get("description", "").strip()
    if not concept or not description:
        return None
    if not _is_valid_concept(concept):
        return None
    return {
        "id": str(uuid.uuid4()),
        "title": concept,
        "type": _infer_type(concept, description),
        "confidence": 0.85,
        "keywords": _build_keywords(concept, description),
        "short_explanation": description,
    }


def _extract_from_chunk(chunk: str, domain: str = "biology") -> List[Dict[str, Any]]:
    extracted = extract_concepts_json(chunk, domain)
    results: List[Dict[str, Any]] = []
    for item in extracted[:MAX_CONCEPTS_PER_CHUNK]:
        concept = _to_pipeline_concept(item)
        if concept is not None:
            results.append(concept)
    return results


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def extract_concepts(chunks: List[str], domain: str = "biology") -> List[Dict[str, Any]]:
    """
    Extract visualizable concepts from a list of text chunks.

    Targets TARGET_CONCEPTS_PER_DOC concepts across all chunks.
    Each chunk yields 2–5 concepts (1 main + supporting).
    Deduplication is applied across chunks.
    """
    if not chunks:
        logger.warning("extract_concepts called with empty chunk list")
        return []

    # ── LLM extraction per chunk ────────────────────────────────────
    normalized_chunks: List[str] = []
    for chunk in chunks:
        normalized_chunks.extend(_split_for_ollama(chunk))

    all_concepts: List[Dict[str, Any]] = []
    seen_titles: set[str] = set()

    for idx, chunk in enumerate(normalized_chunks):
        logger.info(
            "Processing concept chunk %d/%d with local Ollama (%d chars) for domain '%s'",
            idx + 1,
            len(normalized_chunks),
            len(chunk),
            domain
        )

        for concept in _extract_from_chunk(chunk, domain):
            title_key = concept.get("title", "").strip().lower()
            if not title_key or title_key in seen_titles:
                continue
            seen_titles.add(title_key)
            all_concepts.append(concept)

    logger.info(
        "Concept extraction complete: %d concept(s) from %d original chunks (target %d)",
        len(all_concepts),
        len(chunks),
        TARGET_CONCEPTS_PER_DOC,
    )

    return all_concepts
