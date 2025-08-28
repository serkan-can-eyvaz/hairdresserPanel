package com.example.barber.automation.service;

import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.entity.Customer;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.CustomerRepository;
import com.example.barber.automation.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Customer (Müşteri) business logic service
 */
@Service
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    
    @Autowired
    public CustomerService(CustomerRepository customerRepository, TenantRepository tenantRepository) {
        this.customerRepository = customerRepository;
        this.tenantRepository = tenantRepository;
    }
    
    /**
     * Telefon numarasına göre müşteri bulma (WhatsApp entegrasyonu için)
     */
    public Optional<CustomerDto> findByPhoneNumber(String phoneNumber, Long tenantId) {
        return customerRepository.findByPhoneNumberAndTenantId(phoneNumber, tenantId)
                .map(this::convertToDto);
    }
    
    /**
     * Müşteri ID'sine göre bulma
     */
    public Optional<CustomerDto> findById(Long id, Long tenantId) {
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .filter(customer -> customer.getActive())
                .map(this::convertToDto);
    }
    
    /**
     * Tenant'a ait aktif müşterileri listeleme
     */
    public List<CustomerDto> findAllByTenant(Long tenantId) {
        return customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Müşteri adına göre arama
     */
    public List<CustomerDto> searchByName(String name, Long tenantId) {
        return customerRepository.findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(tenantId, name)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni müşteri oluşturma
     */
    public CustomerDto createCustomer(CustomerDto customerDto, Long tenantId) {
        // Tenant kontrolü
        Tenant tenant = tenantRepository.findById(tenantId)
                .filter(t -> t.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Kuaför bulunamadı: " + tenantId));
        
        // Telefon numarası tekrar kontrolü
        if (customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(tenantId, customerDto.getPhoneNumber(), 0L)) {
            throw new IllegalArgumentException("Bu telefon numarası zaten kayıtlı");
        }
        
        Customer customer = convertToEntity(customerDto);
        customer.setTenant(tenant);
        customer.setActive(true);
        customer.setAllowNotifications(true);
        
        Customer savedCustomer = customerRepository.save(customer);
        return convertToDto(savedCustomer);
    }
    
    /**
     * WhatsApp'tan gelen müşteri oluşturma (otomatik)
     */
    public CustomerDto createCustomerFromWhatsApp(String name, String phoneNumber, Long tenantId) {
        // Mevcut müşteri kontrolü
        Optional<Customer> existingCustomer = customerRepository.findByPhoneNumberAndTenantId(phoneNumber, tenantId);
        if (existingCustomer.isPresent()) {
            return convertToDto(existingCustomer.get());
        }
        
        CustomerDto customerDto = new CustomerDto();
        customerDto.setName(name != null ? name : "WhatsApp Müşteri");
        customerDto.setPhoneNumber(phoneNumber);
        customerDto.setAllowNotifications(true);
        
        return createCustomer(customerDto, tenantId);
    }
    
    /**
     * Müşteri güncelleme
     */
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto, Long tenantId) {
        Customer existingCustomer = customerRepository.findByIdAndTenantId(id, tenantId)
                .filter(customer -> customer.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + id));
        
        // Telefon numarası tekrar kontrolü (kendisi hariç)
        if (customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(tenantId, customerDto.getPhoneNumber(), id)) {
            throw new IllegalArgumentException("Bu telefon numarası başka bir müşteri tarafından kullanılıyor");
        }
        
        // Güncelleme
        existingCustomer.setName(customerDto.getName());
        existingCustomer.setPhoneNumber(customerDto.getPhoneNumber());
        existingCustomer.setEmail(customerDto.getEmail());
        existingCustomer.setNotes(customerDto.getNotes());
        if (customerDto.getAllowNotifications() != null) {
            existingCustomer.setAllowNotifications(customerDto.getAllowNotifications());
        }
        
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        return convertToDto(updatedCustomer);
    }
    
    /**
     * Müşteriyi pasif hale getirme (soft delete)
     */
    public void deactivateCustomer(Long id, Long tenantId) {
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + id));
        
        customer.setActive(false);
        customerRepository.save(customer);
    }
    
    /**
     * Hatırlatma için uygun müşterileri bulma
     */
    public List<CustomerDto> findCustomersForReminder(Long tenantId, int reminderDays) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(reminderDays);
        LocalDateTime afterDate = LocalDateTime.now().minusDays(1); // Son 1 gün hariç
        
        return customerRepository.findCustomersForReminder(tenantId, beforeDate, afterDate)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * En sadık müşterileri getirme
     */
    public List<CustomerDto> findMostLoyalCustomers(Long tenantId) {
        return customerRepository.findMostLoyalCustomersByTenantId(tenantId)
                .stream()
                .limit(10) // Top 10
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni müşterileri getirme (son 30 gün)
     */
    public List<CustomerDto> findNewCustomers(Long tenantId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return customerRepository.findNewCustomersSince(tenantId, since)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Müşteri sayısını getirme
     */
    public long countByTenant(Long tenantId) {
        return customerRepository.countByTenantIdAndActiveTrue(tenantId);
    }
    
    // Utility methods
    private CustomerDto convertToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setEmail(customer.getEmail());
        dto.setNotes(customer.getNotes());
        dto.setActive(customer.getActive());
        dto.setAllowNotifications(customer.getAllowNotifications());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        dto.setTenantId(customer.getTenant().getId());
        
        // İstatistikler
        dto.setLastAppointmentDate(customer.getLastAppointmentDate());
        dto.setTotalAppointmentCount(customer.getTotalAppointmentCount());
        
        return dto;
    }
    
    private Customer convertToEntity(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setEmail(dto.getEmail());
        customer.setNotes(dto.getNotes());
        return customer;
    }
}
