from fastapi import APIRouter
from pydantic import BaseModel
from langchain.memory import ConversationBufferWindowMemory
from langchain.chains import ConversationChain
import uuid

from llm_client import get_llm, has_llm

router = APIRouter()


class ChatRequest(BaseModel):
    message: str
    conversation_id: str | None = None
    context: dict | None = None


class ChatResponse(BaseModel):
    reply: str
    conversation_id: str


# Per-conversation memory (last 5 turns). Restart loses memory — OK for demo.
# To persist across restarts, swap with Redis-backed memory later.
_memories: dict[str, ConversationBufferWindowMemory] = {}


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """LLM dialogue with conversation memory (DeepSeek via LangChain)."""
    if not has_llm():
        return ChatResponse(
            reply=f"[stub] DEEPSEEK_API_KEY not set, echo: {request.message}",
            conversation_id=request.conversation_id or "stub",
        )

    cid = request.conversation_id or str(uuid.uuid4())
    if cid not in _memories:
        _memories[cid] = ConversationBufferWindowMemory(k=5, return_messages=True)

    chain = ConversationChain(llm=get_llm(), memory=_memories[cid], verbose=False)
    reply = chain.predict(input=request.message)
    return ChatResponse(reply=reply, conversation_id=cid)


@router.delete("/chat/{conversation_id}")
async def clear_chat(conversation_id: str):
    """Clear conversation memory (for debugging / re-starting a conversation)."""
    _memories.pop(conversation_id, None)
    return {"status": "ok", "cleared": conversation_id}