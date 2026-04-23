import unittest
import sys
import types
from unittest.mock import patch

sys.modules.setdefault("requests", types.SimpleNamespace(post=None))

from modules.api import domain_validator


class DomainValidatorTests(unittest.TestCase):
    def test_biology_heuristic_accepts_common_textbook_terms(self):
        text = (
            "This chapter introduces ecology, evolution, biodiversity, species, "
            "photosynthesis, chromosomes, genetics, and cellular reproduction."
        )

        result = domain_validator._heuristic_validate(text, "biology")

        self.assertTrue(result["match"])
        self.assertEqual(result["detected_domain"], "biology")

    def test_expected_domain_evidence_can_override_llm_mismatch(self):
        text = (
            "Plants use photosynthesis in cells. Genetics explains inherited "
            "traits in species and ecosystems."
        )
        raw_llm_response = (
            '{"detected_domain": "chemistry", "match": false, '
            '"confidence": "high", "reason": "mentions chemical energy"}'
        )

        with patch.object(domain_validator, "generate_with_ollama", return_value=raw_llm_response):
            result = domain_validator.validate_domain(text, "biology")

        self.assertTrue(result["match"])
        self.assertIn("keyword evidence", result["reason"])

    def test_aliases_normalize_for_space_domains(self):
        self.assertTrue(domain_validator._domains_match("astronomy", "stellar"))


if __name__ == "__main__":
    unittest.main()
