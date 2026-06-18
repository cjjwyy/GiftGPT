from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class ChatRequest(BaseModel):
    message: str
    conversation_id: str | None = None
    context: dict | None = None


class ChatResponse(BaseModel):
    reply: str
    conversation_id: str


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """LLM 对话推理 (预留 OpenAI/Qwen/DeepSeek 多模型可插拔)"""
    return ChatResponse(
        reply=f"[LLM Response] 已收到: {request.message}",
        conversation_id=request.conversation_id or "conv-001",
    )
