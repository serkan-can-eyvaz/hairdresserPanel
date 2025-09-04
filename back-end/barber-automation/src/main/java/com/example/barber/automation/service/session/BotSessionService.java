package com.example.barber.automation.service.session;

import com.example.barber.automation.dto.TenantDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BotSessionService {

    public enum BotState {
        INITIAL,
        AWAITING_LOCATION,
        AWAITING_BARBER_SELECTION,
        AWAITING_NAME,
        AWAITING_SERVICE,
        AWAITING_DATE,
        AWAITING_TIME,
        AWAITING_CONFIRMATION,
        COMPLETED
    }

    public static class BotSession {
        private final String phoneNumber;
        private final Long tenantId;
        private BotState state = BotState.INITIAL;
        private Long customerId;
        private Long selectedTenantId;
        private String selectedLocation;
        private List<TenantDto> availableBarbers = new ArrayList<>();
        private LocalDate selectedDate;
        private LocalDateTime selectedTime;
        private List<Long> selectedServiceIds = new ArrayList<>();
        private Integer totalDurationMinutes;
        private String totalCurrency;
        private java.math.BigDecimal totalPrice;

        public BotSession(String phoneNumber, Long tenantId) {
            this.phoneNumber = phoneNumber;
            this.tenantId = tenantId;
        }

        public String key() { return phoneNumber + "_" + tenantId; }

        // getters/setters
        public BotState getState() { return state; }
        public void setState(BotState state) { this.state = state; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public Long getSelectedTenantId() { return selectedTenantId; }
        public void setSelectedTenantId(Long selectedTenantId) { this.selectedTenantId = selectedTenantId; }
        public String getSelectedLocation() { return selectedLocation; }
        public void setSelectedLocation(String selectedLocation) { this.selectedLocation = selectedLocation; }
        public List<TenantDto> getAvailableBarbers() { return availableBarbers; }
        public void setAvailableBarbers(List<TenantDto> availableBarbers) { this.availableBarbers = availableBarbers; }
        public String getPhoneNumber() { return phoneNumber; }
        public Long getTenantId() { return tenantId; }
        public LocalDate getSelectedDate() { return selectedDate; }
        public void setSelectedDate(LocalDate selectedDate) { this.selectedDate = selectedDate; }
        public LocalDateTime getSelectedTime() { return selectedTime; }
        public void setSelectedTime(LocalDateTime selectedTime) { this.selectedTime = selectedTime; }
        public List<Long> getSelectedServiceIds() { return selectedServiceIds; }
        public void setSelectedServiceIds(List<Long> selectedServiceIds) { this.selectedServiceIds = selectedServiceIds; }
        public Integer getTotalDurationMinutes() { return totalDurationMinutes; }
        public void setTotalDurationMinutes(Integer totalDurationMinutes) { this.totalDurationMinutes = totalDurationMinutes; }
        public String getTotalCurrency() { return totalCurrency; }
        public void setTotalCurrency(String totalCurrency) { this.totalCurrency = totalCurrency; }
        public java.math.BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(java.math.BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }

    private final Map<String, BotSession> cache = new ConcurrentHashMap<>();

    public BotSession getOrCreate(String phone, Long tenantId) {
        String key = phone + "_" + tenantId;
        return cache.computeIfAbsent(key, k -> new BotSession(phone, tenantId));
    }
}


