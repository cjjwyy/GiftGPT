from fastapi import APIRouter
from pydantic import BaseModel
from typing import List

from langchain_core.pydantic_v1 import BaseModel as LCBaseModel, Field

from llm_client import get_llm, has_llm

router = APIRouter()


class ProfileAnalyzeRequest(BaseModel):
    recipient_id: int
    recipient_name: str | None = None
    existing_tags: List[str] | None = None
    note: str | None = None
    social_media_data: dict | None = None
    purchase_history: dict | None = None


class ProfileAnalyzeResponse(BaseModel):
    recipient_id: int
    personality_desc: str
    hobbies: List[str]
    suggested_tags: List[str]
    confidence: float


# LangChain structured output schema
class _Profile(LCBaseModel):
    personality_desc: str = Field(description="性格描述，30字以内")
    hobbies: List[str] = Field(description="兴趣列表，5个以内")
    suggested_tags: List[str] = Field(description="建议的兴趣标签，从以下选择：开朗、文艺、极客、养生派、摄影、户外、音乐、运动、美食、咖啡、旅行、阅读、动漫、游戏、宠物、科技、时尚、简约、复古、浪漫、艺术、理性")
    confidence: float = Field(description="置信度0-1", ge=0, le=1)


@router.post("/profile/analyze", response_model=ProfileAnalyzeResponse)
async def analyze_profile(request: ProfileAnalyzeRequest):
    """基于现有画像+消费记录的AI分析 (LangChain → DeepSeek)."""
    if not has_llm():
        return ProfileAnalyzeResponse(
            recipient_id=request.recipient_id,
            personality_desc="LLM 未配置，无法分析",
            hobbies=[],
            suggested_tags=request.existing_tags or [],
            confidence=0.0,
        )

    parts = []
    if request.recipient_name:
        parts.append(f"收礼人姓名：{request.recipient_name}")
    if request.existing_tags:
        parts.append(f"已有标签：{'、'.join(request.existing_tags)}")
    if request.note:
        parts.append(f"备注：{request.note}")
    if request.purchase_history:
        parts.append(f"消费记录：{request.purchase_history}")
    if request.social_media_data:
        parts.append(f"社交信息：{request.social_media_data}")

    info = "\n".join(parts) if parts else "信息较少，请综合推荐通用标签"
    prompt = (
        f"你是一位礼物推荐顾问。请根据以下收礼人信息，分析其性格并推荐1-3个兴趣标签。\n\n"
        f"{info}\n\n"
        f"返回JSON：personality_desc(性格描述)、hobbies(兴趣列表)、suggested_tags(标签)、confidence(置信度)"
    )

    structured_llm = get_llm().with_structured_output(_Profile)
    result: _Profile = structured_llm.invoke(prompt)
    return ProfileAnalyzeResponse(
        recipient_id=request.recipient_id,
        personality_desc=result.personality_desc,
        hobbies=result.hobbies,
        suggested_tags=result.suggested_tags,
        confidence=result.confidence,
    )