"""
RAG Text Chunker
================
Token-aware semantic chunking for the StellAR RAG pipeline.

This implementation uses:
- tiktoken: accurate LLM-style token counting
- spaCy: sentence segmentation
- NLTK: lexical overlap scoring to keep related sentences together
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from functools import lru_cache
from typing import List

import nltk
import spacy
import tiktoken
from nltk.tokenize import wordpunct_tokenize

MIN_CHUNK_TOKENS = 300
MAX_CHUNK_TOKENS = 500
OVERLAP_TOKENS = 20
TOPIC_BREAK_PENALTY = 0.35

_STOP_WORDS = {
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "from",
    "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such",
    "that", "the", "their", "then", "there", "these", "they", "this", "to",
    "was", "will", "with",
}


@dataclass(frozen=True)
class SentenceUnit:
    text: str
    token_count: int
    paragraph_index: int
    keywords: frozenset[str]


def _safe_prepare_nltk() -> None:
    """
    NLTK tokenizers occasionally expect local resources.
    We use wordpunct_tokenize for runtime work, but keeping punkt available
    avoids surprises if this module grows later.
    """
    for resource in ("punkt", "punkt_tab"):
        try:
            nltk.data.find(f"tokenizers/{resource}")
        except LookupError:
            try:
                nltk.download(resource, quiet=True)
            except Exception:
                pass


@lru_cache(maxsize=1)
def _get_encoding():
    try:
        return tiktoken.encoding_for_model("gpt-4o-mini")
    except Exception:
        return tiktoken.get_encoding("cl100k_base")


@lru_cache(maxsize=1)
def _get_spacy_pipeline():
    try:
        nlp = spacy.load("en_core_web_sm", exclude=["tagger", "parser", "ner", "lemmatizer"])
        if "sentencizer" not in nlp.pipe_names:
            nlp.add_pipe("sentencizer")
        return nlp
    except Exception:
        nlp = spacy.blank("en")
        nlp.add_pipe("sentencizer")
        return nlp


def _clean_text(text: str) -> str:
    text = re.sub(r"[^\S\n]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def _count_tokens(text: str) -> int:
    if not text:
        return 0
    return len(_get_encoding().encode(text))


def _extract_keywords(text: str) -> frozenset[str]:
    words = [
        token.lower()
        for token in wordpunct_tokenize(text)
        if token.isalpha() and len(token) > 2
    ]
    return frozenset(word for word in words if word not in _STOP_WORDS)


def _split_paragraphs(text: str) -> List[str]:
    return [paragraph.strip() for paragraph in re.split(r"\n{2,}", text) if paragraph.strip()]


def _sentence_units(text: str) -> List[SentenceUnit]:
    nlp = _get_spacy_pipeline()
    units: List[SentenceUnit] = []

    for paragraph_index, paragraph in enumerate(_split_paragraphs(text)):
        doc = nlp(paragraph)
        for sent in doc.sents:
            sentence = sent.text.strip()
            if not sentence:
                continue
            units.append(
                SentenceUnit(
                    text=sentence,
                    token_count=_count_tokens(sentence),
                    paragraph_index=paragraph_index,
                    keywords=_extract_keywords(sentence),
                )
            )
    return units


def _lexical_similarity(left: SentenceUnit, right: SentenceUnit) -> float:
    if not left.keywords or not right.keywords:
        return 0.0
    overlap = left.keywords & right.keywords
    union = left.keywords | right.keywords
    return len(overlap) / max(len(union), 1)


def _join_sentences(sentences: List[SentenceUnit]) -> str:
    parts: List[str] = []
    previous_paragraph = None

    for sentence in sentences:
        if previous_paragraph is not None and sentence.paragraph_index != previous_paragraph:
            parts.append("\n\n")
        elif parts:
            parts.append(" ")

        parts.append(sentence.text)
        previous_paragraph = sentence.paragraph_index

    return "".join(parts).strip()


def _find_overlap_start(chunk: List[SentenceUnit], global_start: int) -> int:
    remaining = OVERLAP_TOKENS
    overlap_start = global_start + len(chunk)

    for local_index in range(len(chunk) - 1, -1, -1):
        remaining -= chunk[local_index].token_count
        if remaining <= 0:
            overlap_start = global_start + local_index
            break

    return max(overlap_start, global_start + 1)


def _should_keep_overlap(chunk: List[SentenceUnit]) -> bool:
    """
    Overlap is only useful when the tail of the chunk is strongly connected.
    If the chunk already ends at a clean topic boundary, skip overlap entirely
    to reduce redundancy in retrieval.
    """
    if len(chunk) < 2:
        return False
    tail_similarity = _lexical_similarity(chunk[-2], chunk[-1])
    return tail_similarity > TOPIC_BREAK_PENALTY


def chunk_text(text: str) -> List[str]:
    """
    Split raw text into token-bounded, sentence-aligned semantic chunks.
    """
    _safe_prepare_nltk()
    text = _clean_text(text)

    if not text:
        return []

    if _count_tokens(text) <= MAX_CHUNK_TOKENS:
        return [text]

    sentences = _sentence_units(text)
    if not sentences:
        return []

    chunks: List[str] = []
    index = 0
    seen_chunk_texts: set[str] = set()

    while index < len(sentences):
        start_index = index
        current_chunk: List[SentenceUnit] = []
        current_tokens = 0
        cursor = index

        while cursor < len(sentences):
            candidate = sentences[cursor]
            candidate_tokens = current_tokens + candidate.token_count

            if candidate_tokens > MAX_CHUNK_TOKENS and current_tokens >= MIN_CHUNK_TOKENS:
                break

            current_chunk.append(candidate)
            current_tokens = candidate_tokens
            cursor += 1

            if current_tokens < MIN_CHUNK_TOKENS or cursor >= len(sentences):
                continue

            next_sentence = sentences[cursor]
            previous_sentence = current_chunk[-1]
            similarity = _lexical_similarity(previous_sentence, next_sentence)
            paragraph_changed = previous_sentence.paragraph_index != next_sentence.paragraph_index

            if paragraph_changed or similarity <= TOPIC_BREAK_PENALTY:
                break

        if not current_chunk:
            current_chunk.append(sentences[cursor])
            cursor += 1

        chunk = _join_sentences(current_chunk)
        if chunk and chunk not in seen_chunk_texts:
            chunks.append(chunk)
            seen_chunk_texts.add(chunk)

        if cursor >= len(sentences):
            break

        if _should_keep_overlap(current_chunk):
            next_index = _find_overlap_start(current_chunk, index)
        else:
            next_index = cursor

        # Hard guarantee of forward progress to prevent accidental re-chunking
        # of the same sentence range when documents contain highly repetitive text.
        index = max(next_index, start_index + 1)

    return chunks


if __name__ == "__main__":
    sample = """
    The solar system contains the Sun and the objects that orbit it.

    Mercury is the closest planet to the Sun. Venus is the hottest planet
    because of its dense atmosphere. Earth supports life through liquid water.

    Mars is often called the red planet. Jupiter is the largest planet in the
    solar system and has a strong magnetic field.
    """.strip()

    result = chunk_text(sample * 20)
    print(f"Generated {len(result)} chunks")
    print(f"Unique chunks: {len(set(result))}")
    for idx, chunk in enumerate(result, start=1):
        preview = chunk if len(chunk) <= 700 else chunk[:700] + "\n...[truncated]..."
        print(f"\n--- Chunk {idx} ({_count_tokens(chunk)} tokens) ---\n{preview}")
