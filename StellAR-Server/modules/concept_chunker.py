"""
Phase 3 — Concept Chunking Module

Chunks structured PDF sections into concept-boundary-aware groups
based on headings and logical segments, NOT blind token splitting.

This improves:
  - Retrieval accuracy
  - Quiz quality
  - Asset mapping
"""

import logging
from typing import List, Dict, Any

logger = logging.getLogger(__name__)


def chunk_by_concepts(sections: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Group parsed PDF sections into concept chunks based on heading boundaries.
    
    Each chunk contains:
      - concept_title: The heading that starts this concept
      - content_text: All body text under this heading
      - related_figures: List of figure IDs found within this concept
      - page_range: [start_page, end_page]
    
    Args:
        sections: List of section dicts from Phase 1 structural parsing.
                  Each has keys: type, level (optional), text, page

    Returns:
        List of concept chunk dicts
    """
    if not sections:
        return []

    chunks = []
    current_chunk = None

    for section in sections:
        section_type = section.get("type", "paragraph")
        text = section.get("text", "")
        page = section.get("page", 1)

        # A heading or subheading starts a NEW concept chunk
        if section_type in ("heading", "subheading"):
            # Save previous chunk if it exists
            if current_chunk and (current_chunk["content_text"].strip() or current_chunk["related_figures"]):
                _finalize_chunk(current_chunk)
                chunks.append(current_chunk)

            current_chunk = {
                "concept_title": text,
                "content_text": "",
                "related_figures": [],
                "page_range": [page, page],
                "heading_level": section.get("level", 2),
            }

        elif section_type == "paragraph":
            if current_chunk is None:
                # Text before any heading — create an "Introduction" chunk
                current_chunk = {
                    "concept_title": "Introduction",
                    "content_text": "",
                    "related_figures": [],
                    "page_range": [page, page],
                    "heading_level": 1,
                }
            current_chunk["content_text"] += text + "\n\n"
            current_chunk["page_range"][1] = max(current_chunk["page_range"][1], page)

        elif section_type == "figure":
            if current_chunk is None:
                current_chunk = {
                    "concept_title": "Introduction",
                    "content_text": "",
                    "related_figures": [],
                    "page_range": [page, page],
                    "heading_level": 1,
                }
            figure_id = section.get("image_id", "")
            caption = section.get("caption", "")
            if figure_id:
                current_chunk["related_figures"].append({
                    "id": figure_id,
                    "caption": caption,
                })
            current_chunk["page_range"][1] = max(current_chunk["page_range"][1], page)

    # Don't forget the last chunk
    if current_chunk and (current_chunk["content_text"].strip() or current_chunk["related_figures"]):
        _finalize_chunk(current_chunk)
        chunks.append(current_chunk)

    logger.info(f"Chunked {len(sections)} sections into {len(chunks)} concept chunks")
    return chunks


def _finalize_chunk(chunk: Dict[str, Any]):
    """Clean up a chunk before adding it to the result list."""
    chunk["content_text"] = chunk["content_text"].strip()
    # Extract keywords from the concept for downstream asset matching
    chunk["keywords"] = _extract_concept_keywords(chunk["concept_title"], chunk["content_text"])


def _extract_concept_keywords(title: str, content: str, max_keywords: int = 5) -> List[str]:
    """
    Extract the most important keywords from a concept chunk.
    Uses a simple frequency + position heuristic (similar to ocr.py's approach).
    """
    import re
    from collections import Counter

    STOP_WORDS = {
        'the', 'be', 'to', 'of', 'and', 'a', 'in', 'that', 'have', 'i', 'it', 'for',
        'not', 'on', 'with', 'he', 'as', 'you', 'do', 'at', 'this', 'but', 'his',
        'by', 'from', 'they', 'we', 'say', 'her', 'she', 'or', 'an', 'will', 'my',
        'one', 'all', 'would', 'there', 'their', 'what', 'so', 'up', 'out', 'if',
        'about', 'who', 'get', 'which', 'go', 'me', 'is', 'are', 'was', 'were',
        'has', 'had', 'been', 'can', 'could', 'should', 'may', 'might', 'must',
        'shall', 'some', 'any', 'no', 'only', 'own', 'same', 'than', 'too', 'very',
        'just', 'where', 'when', 'why', 'how', 'here', 'also', 'its', 'each',
        'such', 'into', 'other', 'more', 'these', 'those', 'then', 'chapter',
    }

    combined = title + " " + title + " " + content[:2000]  # Double-weight the title
    original_words = re.findall(r'\b[A-Za-z]+\b', combined)
    clean_text = re.sub(r'[^a-zA-Z\s]', '', combined.lower())
    words = clean_text.split()

    scores = Counter()
    for idx, word in enumerate(words):
        if word in STOP_WORDS or len(word) < 3:
            continue
        score = 1
        if idx < 15:
            score += 3  # Title / early position bias
        # Proper noun boost
        for ow in original_words:
            if ow.lower() == word and ow[0].isupper():
                score += 4
                break
        scores[word] += score

    return [word for word, _ in scores.most_common(max_keywords)]
