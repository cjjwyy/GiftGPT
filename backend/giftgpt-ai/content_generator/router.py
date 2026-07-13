from fastapi import APIRouter
from pydantic import BaseModel

from llm_client import get_llm, has_llm

router = APIRouter()


class GreetingRequest(BaseModel):
    recipient_name: str
    relation: str
    occasion: str
    sender_name: str
    style: str = "warm"


class GreetingResponse(BaseModel):
    content: str
    qr_code_url: str | None = None


@router.post("/greeting/generate", response_model=GreetingResponse)
async def generate_greeting(request: GreetingRequest):
    """AI 生成个性化贺卡文案 (LangChain → DeepSeek)."""
    if not has_llm():
        # Fallback: simple template
        content = (
            f"亲爱的{request.recipient_name}，\n\n"
            f"在这个特别的{request.occasion}里，愿这份礼物为你带来温暖与惊喜。"
            f"感谢你一直以来的陪伴，祝你幸福快乐！\n\n"
            f"—— {request.sender_name}"
        )
        return GreetingResponse(content=content, qr_code_url=None)

    style_hint = "温馨亲切" if request.style == "warm" else "文艺含蓄"
    prompt = (
        f"你是一位温暖的礼物祝福语写手。请为以下场景写一段80字以内的贺卡文案，"
        f"语气{style_hint}，只返回贺卡正文，不要引号不要解释：\n"
        f"- 收礼人：{request.recipient_name}\n"
        f"- 与你的关系：{request.relation}\n"
        f"- 送礼场景：{request.occasion}\n"
        f"- 落款人：{request.sender_name}\n"
    )
    content = get_llm().invoke(prompt).content.strip()
    return GreetingResponse(content=content, qr_code_url=None)