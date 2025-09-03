from fastapi import APIRouter, HTTPException
from ...schemas.agent import AgentRespondRequest, AgentRespondResponse
from ...services.nlu import nlu_service

router = APIRouter()

@router.post("/respond", response_model=AgentRespondResponse)
async def respond(request: AgentRespondRequest):
    """Process incoming message and return AI response."""
    try:
        # Process message with NLU service
        result = nlu_service.process_message(
            message=request.message,
            tenant_id=request.tenant_id,
            from_number=request.from_number
        )
        
        # Return response
        return AgentRespondResponse(
            ok=result.get("ok", False),
            intent=result.get("intent", "unknown"),
            reply=result.get("reply", "Mesajınızı aldım. Nasıl yardımcı olabilirim?"),
            next_state=result.get("next_state"),
            extracted_info=result.get("extracted_info", {})
        )
        
    except Exception as e:
        # Log error and return fallback response
        print(f"Error processing message: {e}")
        return AgentRespondResponse(
            ok=False,
            intent="error",
            reply="Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin.",
            next_state="awaiting_name"
        )


