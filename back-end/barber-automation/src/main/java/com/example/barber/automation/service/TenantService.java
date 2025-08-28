package com.example.barber.automation.service;

import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.TenantRepository;
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
    
    @Autowired
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
    
    /**
     * Telefon numarasına göre kuaför bulma (WhatsApp entegrasyonu için)
     */
    public Optional<TenantDto> findByPhoneNumber(String phoneNumber) {
        return tenantRepository.findByPhoneNumber(phoneNumber)
                .map(this::convertToDto);
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
     * Aktif kuaförleri listeleme
     */
    public List<TenantDto> findAllActive() {
        return tenantRepository.findByActiveTrue()
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
        
        Tenant savedTenant = tenantRepository.save(tenant);
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
        return tenantRepository.findByPhoneNumber(phoneNumber)
                .filter(tenant -> tenant.getActive())
                .map(Tenant::getId)
                .orElse(null);
    }
    
    // Utility methods
    private TenantDto convertToDto(Tenant tenant) {
        TenantDto dto = new TenantDto();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setPhoneNumber(tenant.getPhoneNumber());
        dto.setAddress(tenant.getAddress());
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
        tenant.setTimezone(dto.getTimezone() != null ? dto.getTimezone() : "Europe/Istanbul");
        tenant.setEmail(dto.getEmail());
        tenant.setLogoUrl(dto.getLogoUrl());
        return tenant;
    }
}
