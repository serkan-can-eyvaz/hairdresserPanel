package com.example.barber.automation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Müsait saat slot'ları için response DTO
 */
public class SlotResponse {
    
    private LocalDateTime date;
    private List<TimeSlot> availableSlots;
    private int totalSlots;
    private int availableCount;
    
    // Constructors
    public SlotResponse() {}
    
    public SlotResponse(LocalDateTime date, List<TimeSlot> availableSlots) {
        this.date = date;
        this.availableSlots = availableSlots;
        this.totalSlots = availableSlots != null ? availableSlots.size() : 0;
        this.availableCount = totalSlots;
    }
    
    // Getters and Setters
    public LocalDateTime getDate() {
        return date;
    }
    
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    
    public List<TimeSlot> getAvailableSlots() {
        return availableSlots;
    }
    
    public void setAvailableSlots(List<TimeSlot> availableSlots) {
        this.availableSlots = availableSlots;
        this.totalSlots = availableSlots != null ? availableSlots.size() : 0;
        this.availableCount = totalSlots;
    }
    
    public int getTotalSlots() {
        return totalSlots;
    }
    
    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }
    
    public int getAvailableCount() {
        return availableCount;
    }
    
    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }
    
    /**
     * Zaman slot'u için inner class
     */
    public static class TimeSlot {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private boolean available;
        
        public TimeSlot() {}
        
        public TimeSlot(LocalDateTime startTime, LocalDateTime endTime, boolean available) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = available;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
