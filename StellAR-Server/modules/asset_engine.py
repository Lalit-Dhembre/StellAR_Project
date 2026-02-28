"""
Phase 4 — Hybrid Asset Decision Engine

For each concept chunk:
  1. Check Asset DB (Appwrite 'models' collection) for existing 3D models matching concept keywords
  2. If exists → return the asset URL directly
  3. If not → trigger generative fallback:
       Keyword → Search/Reference Image → Vision Validation → Hunyuan 3D → Cache Asset

This makes the system self-expanding over time.
"""

import os
import logging
import uuid
import json
import requests
from typing import List, Dict, Any, Optional

logger = logging.getLogger(__name__)


class AssetEngine:
    """Hybrid Asset Decision Engine — checks DB first, falls back to generation."""

    def __init__(self, app=None):
        self.app = app

    def resolve_assets_for_concepts(self, concepts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        For each concept chunk, find or generate relevant 3D assets.
        
        Returns concepts enriched with an 'assets' field containing matched/generated assets.
        """
        enriched = []
        for concept in concepts:
            keywords = concept.get("keywords", [])
            title = concept.get("concept_title", "")

            # Step 1: Check existing asset DB
            matched_assets = self._search_asset_db(keywords, title)

            if matched_assets:
                logger.info(f"✓ Found {len(matched_assets)} existing assets for '{title}'")
                concept["assets"] = matched_assets
                concept["asset_source"] = "database"
            else:
                logger.info(f"✗ No existing assets for '{title}' — marking for generation")
                concept["assets"] = []
                concept["asset_source"] = "pending_generation"
                # Queue for async generation (Phase 5 handles the actual generation)
                concept["generation_keywords"] = keywords[:3]  # Top 3 keywords for generation

            enriched.append(concept)

        return enriched

    def _search_asset_db(self, keywords: List[str], title: str) -> List[Dict[str, Any]]:
        """
        Query Appwrite 'models' collection for assets matching concept keywords.
        Searches model_name field.
        """
        try:
            from modules.appwrite_service import appwrite_service
            if not appwrite_service.initialized:
                appwrite_service.initialize()
            if not appwrite_service.initialized:
                logger.warning("Appwrite not available for asset lookup")
                return []

            if not appwrite_service.databases:
                return []

            from appwrite.query import Query

            # Search for models whose name matches any keyword
            matched = []
            search_terms = [title.lower()] + [kw.lower() for kw in keywords]

            for term in search_terms:
                try:
                    # Use Query.search for text matching
                    result = appwrite_service.databases.list_documents(
                        database_id=appwrite_service.database_id,
                        collection_id="models",
                        queries=[
                            Query.search("model_name", term),
                            Query.limit(3),
                            Query.select(["model_id", "model_name", "description", "model_url", "model_thumbnail", "rarity"])
                        ]
                    )

                    if result['documents']:
                        for record in result['documents']:
                            # Avoid duplicates
                            if not any(m.get("model_id") == record.get("model_id") for m in matched):
                                matched.append({
                                    "model_id": record.get("model_id"),
                                    "model_name": record.get("model_name"),
                                    "description": record.get("description"),
                                    "model_url": record.get("model_url"),
                                    "thumbnail_url": record.get("model_thumbnail"),
                                    "rarity": record.get("rarity"),
                                    "source": "database",
                                })
                except Exception as e:
                    logger.debug(f"Asset search for term '{term}' failed: {e}")
                    continue

            return matched[:5]  # Cap at 5 assets per concept

        except Exception as e:
            logger.error(f"Asset DB search error: {e}")
            return []

    def trigger_generation_fallback(self, concept: Dict[str, Any], app=None) -> Optional[str]:
        """
        Generative fallback path:
          Keyword → Search for reference image → Queue Hunyuan 3D → Cache asset
        
        This is called asynchronously from Phase 5's orchestration.
        Returns the job_id if generation was queued, None otherwise.
        """
        target_app = app or self.app
        if not target_app:
            logger.error("No Flask app context available for generation")
            return None

        keywords = concept.get("generation_keywords", [])
        title = concept.get("concept_title", "Unknown")

        if not keywords:
            logger.warning(f"No keywords for generation fallback on '{title}'")
            return None

        try:
            comfy_client = target_app.comfy_client
            if not comfy_client:
                logger.warning("ComfyUI client not available for 3D generation")
                return None

            # Use the primary keyword as the generation prompt
            primary_keyword = keywords[0]
            job_id = str(uuid.uuid4())

            logger.info(f"Queuing 3D generation for '{primary_keyword}' (job: {job_id})")

            # For now, log the intent. The actual ComfyUI workflow queueing
            # will be handled by the existing run_generation_task in models.py
            # or a simplified version without requiring an input image.

            return job_id

        except Exception as e:
            logger.error(f"Generation fallback error for '{title}': {e}")
            return None


# Module-level singleton
asset_engine = AssetEngine()
