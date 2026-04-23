from __future__ import annotations

import json
import logging
import os
import re
from types import SimpleNamespace
from typing import Any, Dict, List, Optional

import requests

logger = logging.getLogger(__name__)

OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_GENERATE_URL = f"{OLLAMA_BASE_URL.rstrip('/')}/api/generate"
OLLAMA_MODEL_NAME = os.environ.get("OLLAMA_MODEL_NAME", "qwen3:4b")
DEFAULT_TIMEOUT = int(os.environ.get("OLLAMA_TIMEOUT_SECONDS", "45"))


def extract_json_fragment(raw_text: str) -> Optional[str]:
    """
    Best-effort JSON extraction from model output using regex.
    Returns the first valid array/object fragment that parses successfully.
    """
    text = (raw_text or "").strip()
    if not text:
        return None

    candidates: List[str] = []

    for match in re.finditer(r"```(?:json)?\s*([\s\S]*?)```", text, flags=re.IGNORECASE):
        candidates.append(match.group(1).strip())

    for match in re.finditer(r"\[[\s\S]*\]", text):
        candidates.append(match.group(0).strip())

    for match in re.finditer(r"\{[\s\S]*\}", text):
        candidates.append(match.group(0).strip())

    candidates.append(text)

    seen: set[str] = set()
    for candidate in candidates:
        if not candidate or candidate in seen:
            continue
        seen.add(candidate)
        try:
            json.loads(candidate)
            return candidate
        except json.JSONDecodeError:
            continue

    return None


def generate_with_ollama(
    *,
    prompt: str,
    system: Optional[str] = None,
    model: Optional[str] = None,
    format: Optional[str] = None,
    options: Optional[Dict[str, Any]] = None,
    timeout: Optional[int] = None,
    think: bool = False,
) -> str:
    payload: Dict[str, Any] = {
        "model": model or OLLAMA_MODEL_NAME,
        "prompt": prompt,
        "stream": False,
        "think": think,
        "keep_alive": 0,  # Unload model from VRAM immediately (saves ~2.5 GB for Hunyuan)
    }
    if system:
        payload["system"] = system
    if format:
        payload["format"] = format
    if options:
        payload["options"] = options

    response = requests.post(
        OLLAMA_GENERATE_URL,
        json=payload,
        timeout=timeout or DEFAULT_TIMEOUT,
    )
    response.raise_for_status()
    data = response.json()
    result = (data.get("response") or "").strip()
    # Strip any residual <think> tags that some reasoning models may emit
    result = re.sub(r"<think>[\s\S]*?</think>\s*", "", result).strip()
    return result


class _OllamaCompatCompletions:
    def create(
        self,
        *,
        model: Optional[str] = None,
        messages: Optional[List[Dict[str, str]]] = None,
        temperature: float = 0.2,
        max_completion_tokens: int = 512,
        response_format: Optional[Dict[str, Any]] = None,
        **_: Any,
    ) -> Any:
        system_parts: List[str] = []
        user_parts: List[str] = []

        for message in messages or []:
            role = (message or {}).get("role")
            content = (message or {}).get("content", "")
            if role == "system":
                system_parts.append(content)
            else:
                user_parts.append(content)

        raw = generate_with_ollama(
            prompt="\n\n".join(part for part in user_parts if part),
            system="\n\n".join(part for part in system_parts if part) or None,
            model=model,
            format="json" if response_format else None,
            options={
                "temperature": temperature,
                "num_predict": max_completion_tokens,
            },
        )

        return SimpleNamespace(
            choices=[
                SimpleNamespace(
                    message=SimpleNamespace(content=raw)
                )
            ]
        )


class _OllamaCompatChat:
    def __init__(self) -> None:
        self.completions = _OllamaCompatCompletions()


class OllamaCompatClient:
    """
    Small compatibility shim for legacy Groq-style usage in the codebase.
    """

    def __init__(self) -> None:
        self.chat = _OllamaCompatChat()

