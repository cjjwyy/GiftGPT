from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()


class RecommendRequest(BaseModel):
    recipient_id: int
    personality_tags: list[str]
    occasion: str
    budget: float
    extra_note: Optional[str] = None


class RecommendItem(BaseModel):
    product_id: int
    product_name: str
    price: float
    image_url: str
    platform: str
    platform_url: str
    score: float
    reason: str
    match_tags: list[str]


class RecommendResponse(BaseModel):
    items: list[RecommendItem]
    summary: str


@router.post("/recommend", response_model=RecommendResponse)
async def recommend(request: RecommendRequest):
    """基于画像+场景+预算返回推荐清单 (预留 LLM + 知识图谱集成)"""
    return RecommendResponse(
        items=[
            RecommendItem(
                product_id=1,
                product_name="精致礼物示例",
                price=299.0,
                image_url="https://placeholder.pics/gift/demo",
                platform="京东",
                platform_url="https://www.jd.com",
                score=0.92,
                reason=f"匹配标签: {', '.join(request.personality_tags)}, 场景: {request.occasion}",
                match_tags=request.personality_tags,
            )
        ],
        summary=f"基于 {', '.join(request.personality_tags)} 等特征, "
                f"在 {request.occasion} 场景下为您推荐以上礼物",
    )
