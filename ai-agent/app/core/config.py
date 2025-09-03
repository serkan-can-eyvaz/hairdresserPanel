import os
from dotenv import load_dotenv


class Settings:
    def __init__(self) -> None:
        # Load .env once at startup
        load_dotenv()
        
        # OpenAI API key - sadece environment'dan al
        self.openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
            
        self.openai_model: str = os.getenv("OPENAI_MODEL", "gpt-4o")
        self.openai_temperature: float = float(os.getenv("OPENAI_TEMPERATURE", "0.2"))
        self.backend_url: str = os.getenv("BACKEND_URL", "http://localhost:8080")


settings = Settings()


