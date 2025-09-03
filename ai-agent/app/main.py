from fastapi import FastAPI
from .api.v1.agent import router as agent_router

app = FastAPI(title="AI Agent", version="0.1.0")

@app.get("/health")
def health():
    return {"ok": True, "service": "ai-agent"}

@app.get("/")
def root():
    return {"message": "AI Agent is running!"}

app.include_router(agent_router, prefix="/v1/agent", tags=["agent"])


