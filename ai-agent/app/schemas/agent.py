from pydantic import BaseModel
from typing import Optional, Dict, Any

class AgentRespondRequest(BaseModel):
    tenant_id: int
    from_number: str
    message: str

class AgentRespondResponse(BaseModel):
    ok: bool
    intent: str
    reply: str
    next_state: Optional[str] = None
    extracted_info: Optional[Dict[str, Any]] = None


