package com.example.barber.automation.service;

import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.ServiceRepository;
import com.example.barber.automation.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service (Hizmet) business logic service
 */
@org.springframework.stereotype.Service
@Transactional
public class ServiceService {
    
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;
    
    @Autowired
    public ServiceService(ServiceRepository serviceRepository, TenantRepository tenantRepository) {
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
    }
    
    /**
     * Hizmet ID'sine göre bulma
     */
    public Optional<ServiceDto> findById(Long id, Long tenantId) {
        return serviceRepository.findByIdAndTenantId(id, tenantId)
                .filter(service -> service.getActive())
                .map(this::convertToDto);
    }
    
    /**
     * Tenant'a ait aktif hizmetleri listeleme (sıralı)
     */
    public List<ServiceDto> findAllByTenant(Long tenantId) {
        return serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Tenant'a ait tüm hizmetleri listeleme (pasif dahil)
     */
    public List<ServiceDto> findAllByTenantIncludingInactive(Long tenantId) {
        return serviceRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Hizmet adına göre arama
     */
    public List<ServiceDto> searchByName(String name, Long tenantId) {
        return serviceRepository.findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(tenantId, name)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Süre aralığına göre hizmet bulma
     */
    public List<ServiceDto> findByDurationRange(Integer minDuration, Integer maxDuration, Long tenantId) {
        return serviceRepository.findByTenantIdAndDurationBetween(tenantId, minDuration, maxDuration)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni hizmet oluşturma
     */
    public ServiceDto createService(ServiceDto serviceDto, Long tenantId) {
        // Tenant kontrolü
        Tenant tenant = tenantRepository.findById(tenantId)
                .filter(t -> t.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Kuaför bulunamadı: " + tenantId));
        
        // Hizmet adı tekrar kontrolü
        if (serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, serviceDto.getName(), 0L)) {
            throw new IllegalArgumentException("Bu hizmet adı zaten kayıtlı");
        }
        
        Service service = convertToEntity(serviceDto);
        service.setTenant(tenant);
        service.setActive(true);
        
        // Sıralama için en son sıra numarasını al
        if (service.getSortOrder() == null || service.getSortOrder() == 0) {
            long count = serviceRepository.countByTenantIdAndActiveTrue(tenantId);
            service.setSortOrder((int) count + 1);
        }
        
        Service savedService = serviceRepository.save(service);
        return convertToDto(savedService);
    }
    
    /**
     * Hizmet güncelleme
     */
    public ServiceDto updateService(Long id, ServiceDto serviceDto, Long tenantId) {
        Service existingService = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + id));
        
        // Hizmet adı tekrar kontrolü (kendisi hariç)
        if (serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, serviceDto.getName(), id)) {
            throw new IllegalArgumentException("Bu hizmet adı başka bir hizmet tarafından kullanılıyor");
        }
        
        // Güncelleme
        existingService.setName(serviceDto.getName());
        existingService.setDescription(serviceDto.getDescription());
        existingService.setDurationMinutes(serviceDto.getDurationMinutes());
        existingService.setPrice(serviceDto.getPrice());
        existingService.setCurrency(serviceDto.getCurrency());
        if (serviceDto.getSortOrder() != null) {
            existingService.setSortOrder(serviceDto.getSortOrder());
        }
        
        Service updatedService = serviceRepository.save(existingService);
        return convertToDto(updatedService);
    }
    
    /**
     * Hizmeti pasif hale getirme (soft delete)
     */
    public void deactivateService(Long id, Long tenantId) {
        Service service = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + id));
        
        service.setActive(false);
        serviceRepository.save(service);
    }
    
    /**
     * Hizmeti aktif hale getirme
     */
    public void activateService(Long id, Long tenantId) {
        Service service = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + id));
        
        service.setActive(true);
        serviceRepository.save(service);
    }
    
    /**
     * Hizmet sıralamasını güncelleme
     */
    public void updateServiceOrder(Long id, Integer newOrder, Long tenantId) {
        Service service = serviceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + id));
        
        service.setSortOrder(newOrder);
        serviceRepository.save(service);
    }
    
    /**
     * En popüler hizmetleri getirme
     */
    public List<ServiceDto> findMostPopularServices(Long tenantId) {
        return serviceRepository.findMostPopularServicesByTenantId(tenantId)
                .stream()
                .limit(5) // Top 5
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Hizmet sayısını getirme
     */
    public long countByTenant(Long tenantId) {
        return serviceRepository.countByTenantIdAndActiveTrue(tenantId);
    }
    
    /**
     * WhatsApp botuna hizmet listesi vermek için basit format
     */
    public String getServicesForWhatsApp(Long tenantId) {
        List<ServiceDto> services = findAllByTenant(tenantId);
        
        if (services.isEmpty()) {
            return "Henüz hizmet tanımlanmamış.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔸 *Hizmetlerimiz:*\n\n");
        
        for (int i = 0; i < services.size(); i++) {
            ServiceDto service = services.get(i);
            sb.append(String.format("%d. *%s*\n", i + 1, service.getName()));
            sb.append(String.format("   ⏱️ %d dakika", service.getDurationMinutes()));
            if (service.getPrice() != null) {
                sb.append(String.format(" | 💰 %s", service.getFormattedPrice()));
            }
            sb.append("\n");
            if (service.getDescription() != null && !service.getDescription().trim().isEmpty()) {
                sb.append(String.format("   📝 %s\n", service.getDescription()));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    // Utility methods
    private ServiceDto convertToDto(Service service) {
        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setDurationMinutes(service.getDurationMinutes());
        dto.setPrice(service.getPrice());
        dto.setCurrency(service.getCurrency());
        dto.setActive(service.getActive());
        dto.setSortOrder(service.getSortOrder());
        dto.setCreatedAt(service.getCreatedAt());
        dto.setUpdatedAt(service.getUpdatedAt());
        dto.setTenantId(service.getTenant().getId());
        return dto;
    }
    
    private Service convertToEntity(ServiceDto dto) {
        Service service = new Service();
        service.setName(dto.getName());
        service.setDescription(dto.getDescription());
        service.setDurationMinutes(dto.getDurationMinutes());
        service.setPrice(dto.getPrice());
        service.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "TRY");
        service.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return service;
    }
}
