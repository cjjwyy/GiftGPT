from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class KGQueryRequest(BaseModel):
    cypher: str | None = None
    personality_tag: str | None = None
    occasion: str | None = None
    budget_min: float | None = None
    budget_max: float | None = None


class KGQueryResponse(BaseModel):
    products: list[dict]
    related_tags: list[str]
    reasoning_chain: str


@router.post("/kg/query", response_model=KGQueryResponse)
async def query_knowledge_graph(request: KGQueryRequest):
    """知识图谱多跳推理查询 (预留 Neo4j 集成)"""
    return KGQueryResponse(
        products=[
            {"id": 1, "name": "知识图谱推荐商品", "score": 0.95},
        ],
        related_tags=["文艺", "极客"],
        reasoning_chain="(摄影)→[:SUITABLE_FOR]→(Product:X)→[:FIT_OCCASION]→(生日)",
    )
