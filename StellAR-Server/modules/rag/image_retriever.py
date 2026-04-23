"""
Production-grade educational image retriever for the StellAR backend.

Priority:
    1. Cache
    2. Wikipedia (Fast, Free)
    3. Google Custom Search (High Quality Fallback)
    4. Best-effort fallback (first available image if ranking fails)
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
WIKIPEDIA_THRESHOLD = 0.30       # Lowered: short educational captions score lower
GOOGLE_THRESHOLD = 0.25          # Lowered: Google titles are often generic
MIN_CAPTION_LENGTH = 5           # Lowered from 20: Wikipedia descriptions are often short
MAX_IMAGES = 5
REJECT_TERMS = {"logo", "icon", "symbol", "favicon"}

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
    """
    Rank candidate images by semantic similarity to the concept text.
    Returns the best candidate, or the first valid candidate as fallback.
    """
    if not candidates:
        return {}

    concept_emb = get_embedding(concept_text)
    ranked = []
    first_valid = None  # Track first candidate with a valid URL for fallback

    for img in candidates:
        image_url = img.get("image_url", "")
        caption = img.get("image_caption", "")

        # Track first candidate with a valid URL (for fallback)
        if image_url and first_valid is None:
            first_valid = img

        # Filter 1: missing or very short caption
        if not caption or len(caption.strip()) < MIN_CAPTION_LENGTH:
            # If the caption is too short but we have a URL, use the URL as-is
            # with a low score so it can serve as fallback
            if image_url:
                enriched = dict(img)
                enriched["score"] = 0.1
                ranked.append(enriched)
            continue
            
        # Filter 2: reject terms
        if any(term in caption.lower() for term in REJECT_TERMS):
            continue

        caption_emb = get_embedding(caption)
        sim = cosine_similarity([concept_emb], [caption_emb])[0][0]
        logger.info(f"[Semantic Rank] Score: {sim:.3f} | Caption: '{caption[:100]}'")
        
        enriched = dict(img)
        enriched["score"] = float(sim)
        ranked.append(enriched)

    if not ranked:
        # Absolute fallback: return first valid candidate without ranking
        if first_valid:
            logger.info("[Fallback] No rankable candidates, using first valid image")
            result = dict(first_valid)
            result["score"] = 0.0
            return result
        return {}

    ranked.sort(key=lambda item: item["score"], reverse=True)
    return ranked[0]

# ---------------------------------------------------------
# SOURCE ENGINES
# ---------------------------------------------------------

def fetch_wikipedia_images(title: str) -> List[Dict[str, Any]]:
    """
    Fetch images from Wikipedia for a given concept title.
    Uses generator-based search with thumbnails and page descriptions.
    Includes retry logic for transient failures.
    """
    logger.info(f"Fetching Wikipedia images for: {title}")
    url = "https://en.wikipedia.org/w/api.php"
    params = {
        "action": "query",
        "format": "json",
        "generator": "search",
        "gsrsearch": title,
        "gsrlimit": 5,          # Search more pages for better coverage
        "prop": "pageimages|pageterms",
        "pithumbsize": 1000,
        "pilimit": 5,
    }
    
    # Retry logic: try up to 2 times
    for attempt in range(2):
        try:
            response = requests.get(
                url, 
                params=params, 
                headers={"User-Agent": "StellAR/1.0 (educational-AR-pipeline)"}, 
                timeout=REQUEST_TIMEOUT
            )
            response.raise_for_status()
            data = response.json()
            break
        except Exception as e:
            if attempt == 0:
                logger.debug(f"Wikipedia request attempt 1 failed: {e}, retrying...")
                time.sleep(0.5)
                continue
            logger.warning(f"Wikipedia request failed after retries: {e}")
            return []

    results = []
    pages = data.get("query", {}).get("pages", {})
    
    for page in pages.values():
        img_url = page.get("thumbnail", {}).get("source")
        if not img_url:
            continue

        # Build caption: prefer description, fallback to page title
        terms = page.get("terms", {}).get("description") or []
        if terms and terms[0]:
            caption = terms[0]
        else:
            caption = page.get("title", "")
        
        # Enrich caption with page title if description is very short
        page_title = page.get("title", "")
        if len(caption) < 15 and page_title and caption.lower() != page_title.lower():
            caption = f"{page_title}: {caption}"

        results.append({
            "image_url": img_url,
            "image_caption": caption,
            "source": "wikipedia"
        })
        
    logger.info(f"Wikipedia returned {len(results)} candidate(s) for '{title}'")
    return results[:5]

def fetch_google_images(concept: Dict) -> List[Dict[str, Any]]:
    """
    Fetch images from Google Custom Search API.
    Requires GOOGLE_CUSTOM_SEARCH (API key) and GOOGLE_CX (search engine ID).
    """
    api_key = os.environ.get("GOOGLE_CUSTOM_SEARCH")
    cx = os.environ.get("GOOGLE_CX")
    if not api_key or not cx:
        logger.warning("Google API keys not found (need GOOGLE_CUSTOM_SEARCH and GOOGLE_CX). Skipping Google fallback.")
        return []

    title = concept.get("title", "")
    keywords = " ".join(concept.get("keywords", [])[:3])  # Limit keywords to avoid overly long queries
    query = f"{title} {keywords} educational diagram".strip()
    
    logger.info(f"Fetching Google images for query: {query}")
    url = "https://www.googleapis.com/customsearch/v1"
    params = {
        "q": query,
        "cx": cx,
        "key": api_key,
        "searchType": "image",
        "num": MAX_IMAGES,
        "imgSize": "large",
        "safe": "active",
    }
    
    try:
        response = requests.get(url=url, params=params, timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        data = response.json()
    except requests.exceptions.HTTPError as e:
        # Log specific HTTP errors (quota, auth issues)
        logger.warning(f"Google Search HTTP error: {e.response.status_code} - {e.response.text[:200]}")
        return []
    except Exception as e:
        logger.warning(f"Google Search request failed: {e}")
        return []

    results = []
    items = data.get("items", [])
    for img in items:
        link = img.get("link", "")
        caption = img.get("title", "")
        
        # Skip if no URL
        if not link:
            continue
            
        results.append({
            "image_url": link,
            "image_caption": caption,
            "source": "google"
        })
        
    logger.info(f"Google returned {len(results)} candidate(s) for '{title}'")
    return results

# ---------------------------------------------------------
# CORE PIPELINE
# ---------------------------------------------------------

def retrieve_best_image(concept: Dict[str, Any]) -> Dict[str, Any]:
    """
    Retrieve the best image for a concept using the priority chain:
    Cache → Wikipedia → Google → Fallback.
    """
    title = concept.get("title", "").strip()
    
    if not title:
        logger.warning("retrieve_best_image called with empty title")
        return {"image_url": None, "image_caption": None, "source": "none"}

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

    # 2. WIKIPEDIA LAYER (FREE, FAST)
    wiki_candidates = fetch_wikipedia_images(title)
    best_wiki = rank_images(concept_text, wiki_candidates)

    if best_wiki and best_wiki.get("score", 0.0) >= WIKIPEDIA_THRESHOLD:
        logger.info(f"Wikipedia image accepted for '{title}' (score={best_wiki['score']:.3f} >= {WIKIPEDIA_THRESHOLD})")
        final_result = {
            "image_url": best_wiki["image_url"],
            "image_caption": best_wiki["image_caption"],
            "source": "wikipedia"
        }
        _set_cached_image(title, final_result)
        return final_result

    # 3. GOOGLE API LAYER (PAID FALLBACK)
    logger.info(f"Wikipedia insufficient for '{title}' (best score={best_wiki.get('score', 0):.3f}). Trying Google API.")
    google_candidates = fetch_google_images(concept)
    best_google = rank_images(concept_text, google_candidates)

    if best_google and best_google.get("score", 0.0) >= GOOGLE_THRESHOLD:
        logger.info(f"Google image accepted for '{title}' (score={best_google['score']:.3f} >= {GOOGLE_THRESHOLD})")
        final_result = {
            "image_url": best_google["image_url"],
            "image_caption": best_google["image_caption"],
            "source": "google"
        }
        _set_cached_image(title, final_result)
        return final_result

    # 4. BEST-EFFORT FALLBACK
    # If semantic ranking was too strict, pick the best available image anyway.
    # An image (even imperfect) is better than no image for the AR pipeline.
    fallback_candidate = best_wiki or best_google
    if fallback_candidate and fallback_candidate.get("image_url"):
        logger.info(f"Using best-effort fallback image for '{title}' (score={fallback_candidate.get('score', 0):.3f})")
        final_result = {
            "image_url": fallback_candidate["image_url"],
            "image_caption": fallback_candidate.get("image_caption", title),
            "source": f"{fallback_candidate.get('source', 'unknown')}_fallback"
        }
        _set_cached_image(title, final_result)
        return final_result

    # 5. FINAL FAILURE
    logger.warning(f"No images found for '{title}' across all sources.")
    return {
        "image_url": None,
        "image_caption": None,
        "source": "none"
    }


def retrieve_images(concepts: Any) -> Any:
    """
    Adapter loop to process a single concept or list of concepts.
    Returns concept dicts enriched with image_url, image_caption, and source.
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
        try:
            img_data = retrieve_best_image(concept)
            c_copy.update(img_data)
        except Exception as exc:
            # Never let a single image failure crash the whole batch
            logger.error(f"Image retrieval failed for '{concept.get('title', '?')}': {exc}")
            c_copy["image_url"] = None
            c_copy["image_caption"] = None
            c_copy["source"] = "error"
        results.append(c_copy)
        
    return results
