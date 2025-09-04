"""
AI Agent için DB tool'ları
"""
import httpx
from typing import List, Dict, Any, Optional
from ..core.config import settings


class ToolService:
    """AI Agent'ın kullanabileceği tool'lar"""
    
    def __init__(self):
        self.backend_url = settings.backend_url or "http://localhost:8080"
        self._http_timeout = 10
    
    async def search_tenant(self, query: str) -> Dict[str, Any]:
        """
        Kuaför arama tool'u
        """
        try:
            async with httpx.AsyncClient() as client:
                # Şimdilik basit arama - sonra geliştirilecek
                response = await client.get(f"{self.backend_url}/api/admin/tenants")
                if response.status_code == 200:
                    tenants = response.json()
                    # Basit filtreleme
                    filtered = [t for t in tenants if query.lower() in t.get('name', '').lower()]
                    return {
                        "success": True,
                        "tenants": filtered,
                        "count": len(filtered)
                    }
                else:
                    return {"success": False, "error": f"Backend error: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    async def list_services(self, tenant_id: int) -> Dict[str, Any]:
        """
        Kuaför hizmetlerini listeleme tool'u
        """
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(f"{self.backend_url}/api/services/tenant/{tenant_id}")
                if response.status_code == 200:
                    services = response.json()
                    return {
                        "success": True,
                        "services": services,
                        "count": len(services)
                    }
                else:
                    return {"success": False, "error": f"Backend error: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    async def calculate_duration(self, service_ids: List[int], tenant_id: int) -> Dict[str, Any]:
        """
        Hizmet sürelerini hesaplama tool'u
        """
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(f"{self.backend_url}/api/services/tenant/{tenant_id}")
                if response.status_code == 200:
                    services = response.json()
                    total_duration = 0
                    selected_services = []
                    
                    for service in services:
                        if service.get('id') in service_ids:
                            duration = service.get('durationMinutes', 0)
                            total_duration += duration
                            selected_services.append({
                                'name': service.get('name'),
                                'duration': duration
                            })
                    
                    return {
                        "success": True,
                        "total_duration": total_duration,
                        "services": selected_services,
                        "formatted_duration": f"{total_duration} dakika"
                    }
                else:
                    return {"success": False, "error": f"Backend error: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    async def find_available_slots(self, tenant_id: int, date: str, duration_minutes: int) -> Dict[str, Any]:
        """
        Müsait saatleri bulma tool'u (şimdilik basit)
        """
        try:
            # Şimdilik basit slot hesaplama
            # Sonra gerçek slot engine eklenecek
            working_hours = ["09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"]
            
            # Basit slot hesaplama - gerçek sistemde daha karmaşık olacak
            available_slots = []
            for hour in working_hours:
                # Bu saatte randevu var mı kontrol et (şimdilik hepsi müsait)
                available_slots.append(hour)
            
            return {
                "success": True,
                "date": date,
                "duration_minutes": duration_minutes,
                "available_slots": available_slots,
                "count": len(available_slots)
            }
        except Exception as e:
            return {"success": False, "error": str(e)}
    
    async def create_appointment(self, tenant_id: int, customer_name: str, 
                               service_ids: List[int], date: str, time: str) -> Dict[str, Any]:
        """
        Randevu oluşturma tool'u
        """
        try:
            # Şimdilik basit - sonra gerçek API çağrısı yapılacak
            appointment_data = {
                "tenant_id": tenant_id,
                "customer_name": customer_name,
                "service_ids": service_ids,
                "date": date,
                "time": time,
                "status": "confirmed"
            }
            
            return {
                "success": True,
                "appointment": appointment_data,
                "message": f"Randevu başarıyla oluşturuldu! {customer_name} için {date} {time}"
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    def list_tenants_by_location_sync(self, city: str, district: Optional[str] = None) -> Dict[str, Any]:
        """Şehir/ilçe bazlı kuaförleri backend'den çeker (senkron)."""
        try:
            params = {"city": city}
            if district:
                params["district"] = district
            with httpx.Client() as client:
                response = client.get(f"{self.backend_url}/api/tenants/by-location", params=params, timeout=self._http_timeout)
                if response.status_code == 200:
                    tenants = response.json()
                    return {"success": True, "tenants": tenants, "count": len(tenants)}
                return {"success": False, "error": f"Backend error: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def list_cities_sync(self) -> Dict[str, Any]:
        """Backend'den şehir listesini çeker (senkron)."""
        try:
            with httpx.Client() as client:
                response = client.get(f"{self.backend_url}/api/locations/cities", timeout=self._http_timeout)
                if response.status_code == 200:
                    cities = response.json()
                    return {"success": True, "cities": cities, "count": len(cities)}
                return {"success": False, "error": f"Backend error: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def list_districts_sync(self, city: str) -> Dict[str, Any]:
        """Seçili şehir için ilçe listesini çeker (senkron)."""
        try:
            with httpx.Client() as client:
                response = client.get(f"{self.backend_url}/api/locations/districts", params={"city": city}, timeout=self._http_timeout)
                if response.status_code == 200:
                    districts = response.json()
                    return {"success": True, "districts": districts, "count": len(districts)}
                return {"success": False, "error": f"Backend error: {response.status_code}"}
        except Exception as e:
            return {"success": False, "error": str(e)}


# Global tool service instance - lazy initialization
tool_service = None

def get_tool_service():
    global tool_service
    if tool_service is None:
        tool_service = ToolService()
    return tool_service
