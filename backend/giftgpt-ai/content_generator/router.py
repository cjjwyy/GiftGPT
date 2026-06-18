from fastapi import APIRouter
from pydantic import BaseModel

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
    """AI 生成个性化贺卡文案 (预留 LLM 集成)"""
    content = (
        f"亲爱的 {request.recipient_name}，\n\n"
        f"在这个特别的 {request.occasion} 里，"
        f"愿这份礼物为你带来温暖与惊喜。"
        f"感谢你一直以来的陪伴，祝你幸福快乐！\n\n"
        f"—— {request.sender_name}"
    )
    return GreetingResponse(content=content, qr_code_url=None)
