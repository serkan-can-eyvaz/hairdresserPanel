package com.example.barber.automation.service;

import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.dto.CreateTenantRequest;
import com.example.barber.automation.entity.Tenant;

import com.example.barber.automation.repository.TenantRepository;
import com.example.barber.automation.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tenant (Kuaför) business logic service
 */
@Service
@Transactional
public class TenantService {
    
    private final TenantRepository tenantRepository;
    private final ServiceRepository serviceRepository;
    
    @Autowired
    public TenantService(TenantRepository tenantRepository, ServiceRepository serviceRepository) {
        this.tenantRepository = tenantRepository;
        this.serviceRepository = serviceRepository;
    }
    
    /**
     * Telefon numarasına göre kuaför entity bulma (WhatsApp entegrasyonu için)
     */
    public Tenant findByPhoneNumber(String phoneNumber) {
        return tenantRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }
    
    /**
     * Kuaför ID'sine göre bulma
     */
    public Optional<TenantDto> findById(Long id) {
        return tenantRepository.findById(id)
                .filter(tenant -> tenant.getActive())
                .map(this::convertToDto);
    }
    
    /**
     * Kuaför ID'sine göre entity bulma (WhatsApp entegrasyonu için)
     */
    public Optional<Tenant> findEntityById(Long id) {
        return tenantRepository.findById(id)
                .filter(tenant -> tenant.getActive());
    }
    
    /**
     * Aktif kuaförleri listeleme
     */
    public List<TenantDto> findAllActive() {
        return tenantRepository.findByActiveTrue()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Aktif kuaför entity'lerini listeleme (WhatsApp entegrasyonu için)
     */
    public List<Tenant> findByActiveTrue() {
        return tenantRepository.findByActiveTrue();
    }
    
    /**
     * Şehir ve ilçeye göre aktif kuaförleri bulma
     */
    public List<Tenant> findByCityAndDistrict(String city, String district) {
        return tenantRepository.findByCityAndDistrictAndActiveTrue(city, district);
    }
    
    /**
     * Şehre göre aktif kuaförleri bulma
     */
    public List<Tenant> findByCity(String city) {
        return tenantRepository.findByCityAndActiveTrue(city);
    }
    
    /**
     * Şehir ve ilçeye göre aktif kuaförleri DTO olarak bulma
     */
    public List<TenantDto> findByCityAndDistrictDto(String city, String district) {
        return tenantRepository.findByCityAndDistrictAndActiveTrue(city, district)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Şehre göre aktif kuaförleri DTO olarak bulma
     */
    public List<TenantDto> findByCityDto(String city) {
        return tenantRepository.findByCityAndActiveTrue(city)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Kuaför adına göre arama
     */
    public List<TenantDto> searchByName(String name) {
        return tenantRepository.findByNameContainingIgnoreCaseAndActiveTrue(name)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni kuaför oluşturma
     */
    public TenantDto createTenant(TenantDto tenantDto) {
        // Telefon numarası tekrar kontrolü
        if (tenantRepository.findByPhoneNumber(tenantDto.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Bu telefon numarası zaten kullanılıyor");
        }
        
        Tenant tenant = convertToEntity(tenantDto);
        tenant.setActive(true);
        
        // Konum alanları
        tenant.setCity(tenantDto.getCity());
        tenant.setDistrict(tenantDto.getDistrict());
        tenant.setNeighborhood(tenantDto.getNeighborhood());
        tenant.setAddressDetail(tenantDto.getAddressDetail());

        Tenant savedTenant = tenantRepository.save(tenant);
        return convertToDto(savedTenant);
    }

    /**
     * Hizmetlerle birlikte yeni kuaför oluşturma
     */
    public TenantDto createTenantWithServices(CreateTenantRequest request) {
        // Telefon numarası tekrar kontrolü
        if (tenantRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Bu telefon numarası zaten kullanılıyor");
        }
        
        // Tenant oluştur
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setPhoneNumber(request.getPhoneNumber());
        tenant.setEmail(request.getEmail());
        tenant.setAddress(request.getAddress());
        tenant.setTimezone(request.getTimezone());
        tenant.setCity(request.getCity());
        tenant.setDistrict(request.getDistrict());
        tenant.setNeighborhood(request.getNeighborhood());
        tenant.setAddressDetail(request.getAddressDetail());
        tenant.setActive(true);
        
        Tenant savedTenant = tenantRepository.save(tenant);
        
        // Hizmetleri oluştur
        for (CreateTenantRequest.ServicePrice servicePrice : request.getServices()) {
            com.example.barber.automation.entity.Service originalService = serviceRepository.findById(servicePrice.getServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + servicePrice.getServiceId()));
            
            // Yeni hizmet oluştur (kuaföre özel)
            com.example.barber.automation.entity.Service tenantService = new com.example.barber.automation.entity.Service();
            tenantService.setName(originalService.getName());
            tenantService.setDescription(originalService.getDescription());
            tenantService.setDurationMinutes(originalService.getDurationMinutes());
            tenantService.setPrice(java.math.BigDecimal.valueOf(servicePrice.getPrice()));
            tenantService.setCurrency(servicePrice.getCurrency());
            tenantService.setTenant(savedTenant);
            tenantService.setActive(true);
            tenantService.setSortOrder(originalService.getSortOrder());
            
            serviceRepository.save(tenantService);
        }
        
        return convertToDto(savedTenant);
    }
    
    /**
     * Kuaför güncelleme
     */
    public TenantDto updateTenant(Long id, TenantDto tenantDto) {
        Tenant existingTenant = tenantRepository.findById(id)
                .filter(tenant -> tenant.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Kuaför bulunamadı: " + id));
        
        // Telefon numarası tekrar kontrolü (kendisi hariç)
        if (tenantRepository.existsByPhoneNumberAndIdNot(tenantDto.getPhoneNumber(), id)) {
            throw new IllegalArgumentException("Bu telefon numarası başka bir kuaför tarafından kullanılıyor");
        }
        
        // Email tekrar kontrolü (kendisi hariç)
        if (tenantDto.getEmail() != null && 
            tenantRepository.existsByEmailAndIdNot(tenantDto.getEmail(), id)) {
            throw new IllegalArgumentException("Bu email adresi başka bir kuaför tarafından kullanılıyor");
        }
        
        // Güncelleme
        existingTenant.setName(tenantDto.getName());
        existingTenant.setPhoneNumber(tenantDto.getPhoneNumber());
        existingTenant.setAddress(tenantDto.getAddress());
        existingTenant.setTimezone(tenantDto.getTimezone());
        existingTenant.setEmail(tenantDto.getEmail());
        existingTenant.setLogoUrl(tenantDto.getLogoUrl());
        existingTenant.setCity(tenantDto.getCity());
        existingTenant.setDistrict(tenantDto.getDistrict());
        existingTenant.setNeighborhood(tenantDto.getNeighborhood());
        existingTenant.setAddressDetail(tenantDto.getAddressDetail());
        
        Tenant updatedTenant = tenantRepository.save(existingTenant);
        return convertToDto(updatedTenant);
    }
    
    /**
     * Kuaförü pasif hale getirme (soft delete)
     */
    public void deactivateTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kuaför bulunamadı: " + id));
        
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }
    
    /**
     * Kuaförü aktif hale getirme
     */
    public void activateTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kuaför bulunamadı: " + id));
        
        tenant.setActive(true);
        tenantRepository.save(tenant);
    }
    
    /**
     * Aktif kuaför sayısı
     */
    public long countActiveTenants() {
        return tenantRepository.countActiveTenants();
    }
    
    /**
     * WhatsApp numarasına göre tenant_id bulma (webhook için)
     */
    public Long findTenantIdByWhatsAppNumber(String phoneNumber) {
        // WhatsApp'tan gelen format ile tenant'daki format arasında eşleştirme
        // 1. Direkt arama
        Long tenantId = tenantRepository.findByPhoneNumber(phoneNumber)
                .filter(tenant -> tenant.getActive())
                .map(Tenant::getId)
                .orElse(null);
        
        if (tenantId != null) {
            return tenantId;
        }
        
        // 2. WhatsApp'tan gelen numaraya + ekleyerek arama
        if (!phoneNumber.startsWith("+")) {
            String phoneWithPlus = "+" + phoneNumber;
            tenantId = tenantRepository.findByPhoneNumber(phoneWithPlus)
                    .filter(tenant -> tenant.getActive())
                    .map(Tenant::getId)
                    .orElse(null);
            
            if (tenantId != null) {
                return tenantId;
            }
        }
        
        // 3. Tenant'daki numaradan + çıkararak arama
        if (phoneNumber.startsWith("+")) {
            String phoneWithoutPlus = phoneNumber.substring(1);
            tenantId = tenantRepository.findByPhoneNumber(phoneWithoutPlus)
                    .filter(tenant -> tenant.getActive())
                    .map(Tenant::getId)
                    .orElse(null);
        }
        
        return tenantId;
    }
    
    // Utility methods
    private TenantDto convertToDto(Tenant tenant) {
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setPhoneNumber(tenant.getPhoneNumber());
        dto.setAddress(tenant.getAddress());
        dto.setCity(tenant.getCity());
        dto.setDistrict(tenant.getDistrict());
        dto.setNeighborhood(tenant.getNeighborhood());
        dto.setAddressDetail(tenant.getAddressDetail());
        dto.setTimezone(tenant.getTimezone());
        dto.setEmail(tenant.getEmail());
        dto.setLogoUrl(tenant.getLogoUrl());
        dto.setActive(tenant.getActive());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());
        return dto;
    }
    
    private Tenant convertToEntity(TenantDto dto) {
        Tenant tenant = new Tenant();
        tenant.setName(dto.getName());
        tenant.setPhoneNumber(dto.getPhoneNumber());
        tenant.setAddress(dto.getAddress());
        tenant.setCity(dto.getCity());
        tenant.setDistrict(dto.getDistrict());
        tenant.setNeighborhood(dto.getNeighborhood());
        tenant.setAddressDetail(dto.getAddressDetail());
        tenant.setTimezone(dto.getTimezone() != null ? dto.getTimezone() : "Europe/Istanbul");
        tenant.setEmail(dto.getEmail());
        tenant.setLogoUrl(dto.getLogoUrl());
        return tenant;
    }
}
