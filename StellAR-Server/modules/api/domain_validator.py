"""
Domain Validator Module
Uses Groq LLM to validate whether a document's content matches the expected academic domain.
"""
import os
import json
from groq import Groq

VALID_DOMAINS = ["biology", "chemistry", "physics", "history", "stellar", "space", "astronomy"]

def get_groq_client():
    """Initialize and return a Groq client."""
    api_key = os.environ.get("GROQ_API_KEY")
    if not api_key:
        raise ValueError("GROQ_API_KEY environment variable is not set")
    return Groq(api_key=api_key)


def validate_domain(text: str, expected_domain: str) -> dict:
    """
    Uses Groq LLM to analyze whether a document's text content matches the expected academic domain.
    
    Args:
        text: The extracted text from the document (first ~2000 chars)
        expected_domain: The domain to validate against (e.g. "biology", "chemistry")
    
    Returns:
        dict with keys: match (bool), detected_domain (str), confidence (str), reason (str)
    """
    if not text or not text.strip():
        return {
            "match": False,
            "detected_domain": "unknown",
            "confidence": "low",
            "reason": "No text content found in the document."
        }
    
    # Normalize the expected domain
    expected_domain = expected_domain.lower().strip()
    
    # Map aliases
    domain_aliases = {
        "stellar": "space/astronomy",
        "space": "space/astronomy",
        "astronomy": "space/astronomy",
    }
    display_domain = domain_aliases.get(expected_domain, expected_domain)
    
    # Truncate text to first 2000 characters for efficiency
    text_excerpt = text[:2000]
    
    prompt = f"""You are an academic document classifier. Analyze the following text excerpt from a document and determine which academic domain it belongs to.

The valid domains are: Biology, Chemistry, Physics, History, Space/Astronomy.

Text excerpt:
\"\"\"
{text_excerpt}
\"\"\"

Expected domain: {display_domain}

Respond ONLY with a valid JSON object (no markdown, no code fences) in this exact format:
{{
    "detected_domain": "<the domain this text belongs to>",
    "match": <true if the detected domain matches '{display_domain}', false otherwise>,
    "confidence": "<high, medium, or low>",
    "reason": "<brief 1 sentence explanation>"
}}"""

    try:
        client = get_groq_client()
        
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {
                    "role": "system",
                    "content": "You are a precise academic document classifier. Always respond with valid JSON only."
                },
                {
                    "role": "user", 
                    "content": prompt
                }
            ],
            temperature=0.1,
            max_completion_tokens=200,
            response_format={"type": "json_object"}
        )
        
        result_text = response.choices[0].message.content.strip()
        print(f"[DomainValidator] Groq raw response: {result_text}")
        
        result = json.loads(result_text)
        
        # Ensure required keys exist
        return {
            "match": result.get("match", False),
            "detected_domain": result.get("detected_domain", "unknown"),
            "confidence": result.get("confidence", "low"),
            "reason": result.get("reason", "Could not determine reason.")
        }
        
    except json.JSONDecodeError as e:
        print(f"[DomainValidator] JSON parse error: {e}")
        return {
            "match": False,
            "detected_domain": "unknown",
            "confidence": "low",
            "reason": f"Failed to parse LLM response: {e}"
        }
    except Exception as e:
        print(f"[DomainValidator] Error: {e}")
        return {
            "match": False,
            "detected_domain": "unknown",
            "confidence": "low",
            "reason": f"Validation error: {str(e)}"
        }
