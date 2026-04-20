import requests
import re
import logging
from typing import List, Dict

logger = logging.getLogger(__name__)

def fetch_duckduckgo_images(query: str) -> List[Dict]:
    """
    Fetches the top 5 image results from DuckDuckGo for a given query.
    Implements a 1-retry fallback natively.
    """
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept": "application/json"
    }

    # Step 6: Error Handling - Retry once if request fails
    for attempt in range(2):
        try:
            # Step 1: Get vqd token
            res = requests.get("https://duckduckgo.com/", params={"q": query}, headers=headers, timeout=10)
            res.raise_for_status()
            
            # Extract vqd token. DuckDuckGo passes it either in script tags or form inputs.
            match = re.search(r'vqd=([^&\'"\s]+)', res.text)
            if not match:
                logger.debug(f"Could not find vqd token on attempt {attempt+1}")
                continue
                
            vqd = match.group(1)
            
            # Step 2: Fetch image results
            params = {
                "q": query,
                "vqd": vqd,
                "o": "json"
            }

            # Step 3 is satisfied by using `headers` implicitly above.
            script_url = "https://duckduckgo.com/i.js"
            image_res = requests.get(script_url, params=params, headers=headers, timeout=10)
            image_res.raise_for_status()
            
            # Step 4: Parse response
            data = image_res.json()
            results = data.get("results", [])
            
            extracted_images = []
            
            # Step 5: Limit results to top 5 images
            for img in results[:5]:
                extracted_images.append({
                    "url": img.get("image", ""),
                    "thumbnail": img.get("thumbnail", ""),
                    "title": img.get("title", "")
                })
                
            return extracted_images
            
        except Exception as e:
            if attempt == 1:
                logger.warning(f"DuckDuckGo fetch failed after retries for '{query}': {e}")
                return []
                
    # Return empty list if no results
    return []
