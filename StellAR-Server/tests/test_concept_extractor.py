import sys
import types
import unittest
from unittest.mock import patch

sys.modules.setdefault("requests", types.SimpleNamespace(post=None))

from modules.rag import concept_extractor


class ConceptExtractorFallbackTests(unittest.TestCase):
    def test_ollama_error_uses_local_fallback(self):
        chunk = (
            "Photosynthesis takes place in the chloroplast of plant cells. "
            "Chlorophyll helps leaves capture light energy for making food."
        )

        with patch.object(concept_extractor, "generate_with_ollama", side_effect=RuntimeError("500")):
            concepts = concept_extractor.extract_concepts_json(chunk)

        titles = {item["concept"] for item in concepts}
        self.assertIn("Photosynthesis", titles)
        self.assertIn("Chloroplast", titles)

    def test_empty_model_output_uses_local_fallback(self):
        chunk = (
            "The heart pumps blood through blood vessels. The circulatory "
            "system transports materials throughout the body."
        )

        with patch.object(concept_extractor, "generate_with_ollama", return_value="not json"):
            concepts = concept_extractor.extract_concepts_json(chunk)

        self.assertTrue(any(item["concept"] == "Circulatory System" for item in concepts))
        self.assertTrue(all(item["description"] for item in concepts))

    def test_pipeline_concepts_are_created_from_fallback_items(self):
        chunk = "Digestion occurs in the stomach and intestine with the help of enzymes."

        with patch.object(concept_extractor, "generate_with_ollama", side_effect=RuntimeError("500")):
            concepts = concept_extractor.extract_concepts([chunk])

        self.assertTrue(concepts)
        self.assertTrue({"id", "title", "type", "keywords", "short_explanation"} <= concepts[0].keys())


if __name__ == "__main__":
    unittest.main()
