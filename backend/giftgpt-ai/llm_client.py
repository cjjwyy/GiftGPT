"""Shared LLM client: LangChain ChatOpenAI wrapper over DeepSeek API.

All routers import `llm` from here. To switch model/provider, edit only this file.
"""

import os
from typing import Optional

from langchain_openai import ChatOpenAI


def _build_llm() -> ChatOpenAI:
    api_key = os.getenv("DEEPSEEK_API_KEY", "")
    base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")
    model = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
    return ChatOpenAI(
        model=model,
        api_key=api_key,
        base_url=base_url,
        temperature=0.7,
        max_tokens=2048,
    )


# Lazy init: only create on first use to allow routers without keys (stub mode)
_llm: Optional[ChatOpenAI] = None


def get_llm() -> ChatOpenAI:
    """Return shared LLM. Fallback returns None if key is absent."""
    global _llm
    if not os.getenv("DEEPSEEK_API_KEY"):
        raise RuntimeError("DEEPSEEK_API_KEY not set in environment")
    if _llm is None:
        _llm = _build_llm()
    return _llm


def has_llm() -> bool:
    return bool(os.getenv("DEEPSEEK_API_KEY"))