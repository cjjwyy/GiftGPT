from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

load_dotenv()

from llm_service.router import router as llm_router
from profile_analyzer.router import router as profile_router
from recommender.router import router as recommender_router
from kg_service.router import router as kg_router
from content_generator.router import router as content_router

app = FastAPI(
    title="GiftGPT AI Service",
    description="AI推理、画像分析、推荐引擎、知识图谱、内容生成",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(llm_router, prefix="/api/v1/ai", tags=["LLM Service"])
app.include_router(profile_router, prefix="/api/v1/ai", tags=["Profile Analyzer"])
app.include_router(recommender_router, prefix="/api/v1/ai", tags=["Recommender"])
app.include_router(kg_router, prefix="/api/v1/ai", tags=["Knowledge Graph"])
app.include_router(content_router, prefix="/api/v1/ai", tags=["Content Generator"])


@app.get("/health")
async def health():
    return {"status": "ok", "service": "giftgpt-ai"}
