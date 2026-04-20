"""
Production-grade educational image retriever for the StellAR backend.

Priority:
    1. Cache
    2. Wikipedia (Fast, Cheap)
    3. Google Custom Search (High Quality Fallback)
"""

import json
import logging
import os
import re
import time
from typing import Any, Dict, Iterable, List, Sequence, Optional

import requests
import redis
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

logger = logging.getLogger(__name__)

# System Constants
REQUEST_TIMEOUT = 10
WIKIPEDIA_THRESHOLD = 0.45
GOOGLE_THRESHOLD = 0.35
MAX_IMAGES = 5
REJECT_TERMS = {"logo", "icon", "symbol"}

# Global Models & Redis Config
_embedding_model: Optional[SentenceTransformer] = None
_embedding_cache: Dict[str, Any] = {}

REDIS_URL = os.environ.get("REDIS_URL") or os.environ.get("CELERY_BROKER_URL") or "redis://localhost:6379/0"
_image_cache: Dict[str, Dict[str, Any]] = {}
_redis_client: Optional[redis.Redis] = None
_redis_disabled = False

# ---------------------------------------------------------
# CACHE CONFIGURATION
# ---------------------------------------------------------

def _cache_key(title: str) -> str:
    return re.sub(r"\s+", " ", title.strip().lower())

def _get_redis_client() -> Optional[redis.Redis]:
    global _redis_client, _redis_disabled
    if _redis_disabled:
        return None
    if _redis_client is not None:
        return _redis_client
    try:
        client = redis.Redis.from_url(REDIS_URL, decode_responses=True, socket_connect_timeout=2)
        client.ping()
        _redis_client = client
        return _redis_client
    except Exception as exc:
        logger.warning(f"Redis cache unavailable, falling back to local memory: {exc}")
        _redis_disabled = True
        return None

def _get_cached_image(title: str) -> Optional[Dict[str, Any]]:
    redis_client = _get_redis_client()
    key = f"stellar:img:{_cache_key(title)}"
    if redis_client:
        try:
            payload = redis_client.get(key)
            if payload:
                return json.loads(payload)
        except Exception:
            pass
    return _image_cache.get(_cache_key(title))

def _set_cached_image(title: str, value: Dict[str, Any]) -> None:
    redis_client = _get_redis_client()
    key = f"stellar:img:{_cache_key(title)}"
    _image_cache[_cache_key(title)] = value
    if redis_client:
        try:
            redis_client.setex(key, 86400, json.dumps(value))
        except Exception:
            pass

# ---------------------------------------------------------
# EMBEDDING RANKING
# ---------------------------------------------------------

def get_embedding(text: str) -> Any:
    global _embedding_model
    text = re.sub(r"\s+", " ", (text or "").strip().lower())
    if text in _embedding_cache:
        return _embedding_cache[text]
    
    if _embedding_model is None:
        logger.info("Loading sentence-transformers: all-MiniLM-L6-v2")
        _embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
        
    emb = _embedding_model.encode([text])[0]
    _embedding_cache[text] = emb
    return emb

def rank_images(concept_text: str, candidates: List[Dict[str, Any]]) -> Dict[str, Any]:
    if not candidates:
        return {}

    concept_emb = get_embedding(concept_text)
    ranked = []

    for img in candidates:
        caption = img.get("image_caption", "")
        # Filter 1: missing caption
        if not caption:
            continue
        
        # Filter 2: short caption
        if len(caption.strip()) < 20:
            continue
            
        # Filter 3: reject terms
        if any(term in caption.lower() for term in REJECT_TERMS):
            continue

        caption_emb = get_embedding(caption)
        sim = cosine_similarity([concept_emb], [caption_emb])[0][0]
        logger.info(f"[Semantic Rank] Score: {sim:.3f} | Caption: '{caption[:100]}...'")
        
        enriched = dict(img)
        enriched["score"] = float(sim)
        ranked.append(enriched)

    if not ranked:
        return {}

    ranked.sort(key=lambda item: item["score"], reverse=True)
    return ranked[0]

# ---------------------------------------------------------
# SOURCE ENGINES
# ---------------------------------------------------------

def fetch_wikipedia_images(title: str) -> List[Dict[str, Any]]:
    logger.info(f"Fetching Wikipedia images for: {title}")
    url = "https://en.wikipedia.org/w/api.php"
    params = {
        "action": "query",
        "format": "json",
        "generator": "search",
        "gsrsearch": title,
        "gsrlimit": 3,
        "prop": "pageimages|pageterms",
        "pithumbsize": 1000,
        "pilimit": 3,
    }
    
    try:
        response = requests.get(
            url, 
            params=params, 
            headers={"User-Agent": "Mozilla/5.0"}, 
            timeout=REQUEST_TIMEOUT
        )
        response.raise_for_status()
        data = response.json()
    except Exception as e:
        logger.warning(f"Wikipedia request failed: {e}")
        return []

    results = []
    pages = data.get("query", {}).get("pages", {})
    
    for page in pages.values():
        img_url = page.get("thumbnail", {}).get("source")
        if not img_url:
            continue

        terms = page.get("terms", {}).get("description") or [""]
        caption = terms[0] or page.get("title", "")
        
        results.append({
            "image_url": img_url,
            "image_caption": caption,
            "source": "wikipedia"
        })
        
    return results[:3]

def fetch_google_images(concept: Dict) -> List[Dict[str, Any]]:
    # Requires GOOGLE_CUSTOM_SEARCH and GOOGLE_CX
    api_key = os.environ.get("GOOGLE_CUSTOM_SEARCH")
    cx = os.environ.get("GOOGLE_CX")
    if not api_key or not cx:
        logger.warning("Google API keys not found. Skipping Google fallback.")
        return []

    title = concept.get("title", "")
    keywords = " ".join(concept.get("keywords", []))
    query = f"{title} {keywords} labeled diagram".strip()
    
    logger.info(f"Fetching Google images for query: {query}")
    url = "https://www.googleapis.com/customsearch/v1"
    params = {
        "q": query,
        "cx": cx,
        "key": api_key,
        "searchType": "image",
        "num": MAX_IMAGES
    }
    
    try:
        response = requests.get(url=url, params=params, timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        data = response.json()
    except Exception as e:
        logger.warning(f"Google Search request failed: {e}")
        return []

    results = []
    items = data.get("items", [])
    for img in items:
        results.append({
            "image_url": img.get("link"),
            "image_caption": img.get("title", ""),
            "source": "google"
        })
        
    return results

# ---------------------------------------------------------
# CORE PIPELINE
# ---------------------------------------------------------

def retrieve_best_image(concept: Dict[str, Any]) -> Dict[str, Any]:
    title = concept.get("title", "").strip()
    
    # 1. CACHE LAYER (FIRST PRIORITY)
    cached = _get_cached_image(title)
    if cached:
        logger.info(f"Image for '{title}' found in cache.")
        cached["source"] = "cache"
        return cached

    # Build embedding target string
    keywords = concept.get("keywords", [])
    explanation = concept.get("short_explanation", "")
    concept_text = f"{title}. Keywords: {', '.join(keywords)}. {explanation}"

    # 2. WIKIPEDIA LAYER (CHEAP ATTEMPT)
    wiki_candidates = fetch_wikipedia_images(title)
    best_wiki = rank_images(concept_text, wiki_candidates)

    if best_wiki and best_wiki.get("score", 0.0) >= WIKIPEDIA_THRESHOLD:
        logger.info(f"Wikipedia triggered successfully for '{title}' (score >= {WIKIPEDIA_THRESHOLD})")
        final_result = {
            "image_url": best_wiki["image_url"],
            "image_caption": best_wiki["image_caption"],
            "source": "wikipedia"
        }
        _set_cached_image(title, final_result)
        return final_result

    # 5. GOOGLE API LAYER (FALLBACK)
    logger.info(f"Wikipedia fallback triggered for '{title}'. Falling back to Google API.")
    google_candidates = fetch_google_images(concept)
    best_google = rank_images(concept_text, google_candidates)

    if best_google and best_google.get("score", 0.0) >= GOOGLE_THRESHOLD:
        logger.info(f"Google triggered successfully for '{title}' (score >= {GOOGLE_THRESHOLD})")
        final_result = {
            "image_url": best_google["image_url"],
            "image_caption": best_google["image_caption"],
            "source": "google"
        }
        _set_cached_image(title, final_result)
        return final_result

    # 7. FINAL SELECTION FAILED
    logger.warning(f"No suitable images found for '{title}' across all sources.")
    failed_result = {
        "image_url": None,
        "image_caption": None,
        "source": "none"
    }
    return failed_result


def retrieve_images(concepts: Any) -> Any:
    """
    Adapter loop to securely serve pipeline lists.
    """
    if isinstance(concepts, dict):
        base = dict(concepts)
        img_data = retrieve_best_image(concepts)
        base.update(img_data)
        return base

    if not isinstance(concepts, Iterable):
        raise TypeError("retrieve_images expects a concept dict or iterable.")

    results = []
    for concept in concepts:
        if not isinstance(concept, dict):
            continue
        c_copy = dict(concept)
        img_data = retrieve_best_image(concept)
        c_copy.update(img_data)
        results.append(c_copy)
        
    return results
