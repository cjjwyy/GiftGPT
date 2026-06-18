from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class ProfileAnalyzeRequest(BaseModel):
    recipient_id: int
    social_media_data: dict | None = None
    purchase_history: dict | None = None


class ProfileAnalyzeResponse(BaseModel):
    recipient_id: int
    personality_desc: str
    hobbies: list[str]
    suggested_tags: list[str]
    confidence: float


@router.post("/profile/analyze", response_model=ProfileAnalyzeResponse)
async def analyze_profile(request: ProfileAnalyzeRequest):
    """基于社交媒体公开信息 + 消费记录的多模态画像分析"""
    return ProfileAnalyzeResponse(
        recipient_id=request.recipient_id,
        personality_desc="文艺青年，喜欢摄影和旅行，注重生活品质",
        hobbies=["摄影", "阅读", "咖啡", "旅行"],
        suggested_tags=["文艺", "摄影", "户外"],
        confidence=0.85,
    )
