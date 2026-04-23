"""
Domain Validator Module
Uses a local Ollama model first, then falls back to keyword heuristics so the
RAG pipeline stays offline-capable and does not fail closed during demos.
"""

from __future__ import annotations

import json
import os
import re
from typing import Dict, List

from modules.local_llm import OLLAMA_MODEL_NAME, OllamaCompatClient, extract_json_fragment, generate_with_ollama

VALID_DOMAINS = ["biology", "chemistry", "physics", "history", "stellar", "space", "astronomy"]
GROQ_MODEL_NAME = OLLAMA_MODEL_NAME

_DOMAIN_KEYWORDS: Dict[str, List[str]] = {
    "biology": [
        "animal",
        "biodiversity",
        "blood",
        "botany",
        "cell",
        "cellular",
        "chromosome",
        "dna",
        "ecology",
        "ecosystem",
        "evolution",
        "gene",
        "genetic",
        "genetics",
        "heart",
        "homeostasis",
        "microorganism",
        "organ",
        "organism",
        "photosynthesis",
        "physiology",
        "plant",
        "reproduction",
        "species",
        "tissue",
        "zoology",
    ],
    "chemistry": ["atom", "molecule", "reaction", "compound", "element", "acid", "bond"],
    "physics": ["force", "motion", "energy", "velocity", "magnet", "gravity", "wave"],
    "history": ["empire", "civilization", "war", "king", "century", "revolution", "dynasty"],
    "space/astronomy": ["planet", "star", "galaxy", "orbit", "solar", "moon", "astronomy"],
}


def get_groq_client():
    """
    Legacy compatibility shim for old imports/callers.
    """
    return OllamaCompatClient()


def _normalize_domain(expected_domain: str) -> str:
    expected = (expected_domain or "").lower().strip()
    aliases = {
        "life science": "biology",
        "life sciences": "biology",
        "bio": "biology",
        "stellar": "space/astronomy",
        "space": "space/astronomy",
        "astronomy": "space/astronomy",
        "space science": "space/astronomy",
    }
    return aliases.get(expected, expected)


def _domain_scores(text: str) -> Dict[str, int]:
    lowered = (text or "").lower()
    scores: Dict[str, int] = {}
    for domain, keywords in _DOMAIN_KEYWORDS.items():
        score = 0
        for keyword in keywords:
            score += len(re.findall(rf"\b{re.escape(keyword)}s?\b", lowered))
        scores[domain] = score
    return scores


def _domains_match(detected_domain: str, expected_domain: str) -> bool:
    return _normalize_domain(detected_domain) == _normalize_domain(expected_domain)


def _heuristic_validate(text: str, expected_domain: str) -> dict:
    normalized_domain = _normalize_domain(expected_domain)
    scores = _domain_scores(text)

    best_domain = max(scores, key=scores.get) if scores else "unknown"
    best_score = scores.get(best_domain, 0)
    expected_score = scores.get(normalized_domain, 0)

    if best_score == 0:
        return {
            "match": True,
            "detected_domain": normalized_domain or "unknown",
            "confidence": "low",
            "reason": "Local validator could not classify confidently, so the document was allowed through.",
        }

    return {
        "match": best_domain == normalized_domain or expected_score >= 2,
        "detected_domain": best_domain,
        "confidence": "medium" if best_score >= 3 else "low",
        "reason": f"Keyword heuristic matched {best_domain}; expected-domain score was {expected_score}.",
    }


def validate_domain(text: str, expected_domain: str) -> dict:
    if not text or not text.strip():
        return {
            "match": False,
            "detected_domain": "unknown",
            "confidence": "low",
            "reason": "No text content found in the document.",
        }

    normalized_domain = _normalize_domain(expected_domain)
    text_excerpt = text[:2000]

    prompt = f"""Classify the academic domain of this text excerpt.

Valid domains: biology, chemistry, physics, history, space/astronomy.
Expected domain: {normalized_domain}

Return ONLY valid JSON in this exact shape:
{{
  "detected_domain": "string",
  "match": true,
  "confidence": "high",
  "reason": "short sentence"
}}

Text excerpt:
\"\"\"
{text_excerpt}
\"\"\""""

    try:
        raw = generate_with_ollama(
            prompt=prompt,
            system="You are a precise academic document classifier. Return JSON only.",
            model=OLLAMA_MODEL_NAME,
            format="json",
            options={
                "temperature": 0.1,
                "num_predict": 200,
            },
            timeout=20,
        )

        json_fragment = extract_json_fragment(raw)
        if not json_fragment:
            return _heuristic_validate(text_excerpt, expected_domain)

        data = json.loads(json_fragment)
        detected_domain = data.get("detected_domain") or "unknown"
        llm_match = bool(data.get("match", _domains_match(detected_domain, normalized_domain)))
        if not llm_match:
            heuristic = _heuristic_validate(text_excerpt, expected_domain)
            if heuristic.get("match"):
                return {
                    "match": True,
                    "detected_domain": heuristic.get("detected_domain", normalized_domain),
                    "confidence": heuristic.get("confidence", "low"),
                    "reason": (
                        "Local keyword evidence matched the expected domain despite "
                        "the model returning a mismatch."
                    ),
                }

        return {
            "match": llm_match,
            "detected_domain": _normalize_domain(detected_domain),
            "confidence": data.get("confidence", "low"),
            "reason": data.get("reason", "Local validator completed without a detailed reason."),
        }
    except Exception:
        return _heuristic_validate(text_excerpt, expected_domain)
