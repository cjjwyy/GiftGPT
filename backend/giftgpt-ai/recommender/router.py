from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional, List

from langchain_core.pydantic_v1 import BaseModel as LCBaseModel, Field

from llm_client import get_llm, has_llm

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
    reasoning_chain: Optional[str] = None


class RecommendResponse(BaseModel):
    items: list[RecommendItem]
    summary: str


# LangChain structured output schema
class _Gift(LCBaseModel):
    name: str = Field(description="礼物名称，含品牌/型号，便于搜索")
    price: float = Field(description="价格数字")
    reason: str = Field(description='25字以内推荐理由，以动词+称谓开头，如"送妈妈一束永生花"')
    tags: List[str] = Field(description="匹配的兴趣标签列表")
    platform: str = Field(description="购买平台", default="拼多多")


class _GiftList(LCBaseModel):
    gifts: List[_Gift] = Field(description="5-8件推荐礼物")
    summary: str = Field(description="50字以内推荐策略总结")


_SYSTEM = (
    "你是一位温暖细腻的礼物推荐AI助手。你总是以JSON格式回复，不添加任何额外的解释或markdown标记。"
    "你推荐的礼物贴近生活、实用且有情感价值，语言柔和温暖，善于用收礼人的称谓让每份推荐都更有温度。"
)


def _build_prompt(req: RecommendRequest) -> str:
    tags_str = "、".join(req.personality_tags) if req.personality_tags else "暂无标签"
    extra = f"【额外说明】{req.extra_note}\n" if req.extra_note else ""
    return (
        f"请基于下方收礼人的完整画像，在指定场景和预算内，推荐 5-8 件最合适的礼物。\n"
        f"\n"
        f"【收礼人标签】{tags_str}\n"
        f"【送礼场景】{req.occasion}\n"
        f"【预算】¥{req.budget}（推荐价格应在预算的60%-100%之间，不要远低于预算）\n"
        f"{extra}"
        f"\n"
        f"【挑选原则】\n"
        f"1. 综合考虑关系、性别、年龄段、MBTI、性格特点、兴趣标签等画像信息；\n"
        f"2. 兼顾情感价值与实用性；\n"
        f"3. 价格尽量接近预算（60%-100%），不要推荐远低于预算的廉价品；\n"
        f"4. 推荐理由须在25字以内，以\"动词+称谓\"开头，语言柔和温暖。\n"
    )


@router.post("/recommend", response_model=RecommendResponse)
async def recommend(request: RecommendRequest):
    """基于画像+场景+预算返回推荐清单 (LangChain structured output)."""
    if not has_llm():
        return RecommendResponse(
            items=[RecommendItem(
                product_id=0, product_name="LLM 未配置", price=0.0, image_url="",
                platform="", platform_url="", score=0.0,
                reason="设置 DEEPSEEK_API_KEY 后可用",
                match_tags=request.personality_tags, reasoning_chain=None,
            )],
            summary="DEEPSEEK_API_KEY not set, returning stub",
        )

    llm = get_llm()
    structured_llm = llm.with_structured_output(_GiftList)
    prompt = _build_prompt(request)
    result: _GiftList = structured_llm.invoke([
        {"role": "system", "content": _SYSTEM},
        {"role": "user", "content": prompt},
    ])

    items = [
        RecommendItem(
            product_id=0,
            product_name=g.name,
            price=g.price,
            image_url="",
            platform=g.platform or "拼多多",
            platform_url="",
            score=0.90 + 0.05 * min(len(g.tags), 4) / 4.0,
            reason=g.reason,
            match_tags=g.tags,
            reasoning_chain=None,
        )
        for g in result.gifts
    ]
    return RecommendResponse(items=items, summary=result.summary)