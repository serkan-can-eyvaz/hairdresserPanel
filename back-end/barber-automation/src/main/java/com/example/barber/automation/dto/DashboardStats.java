package com.example.barber.automation.dto;

import java.time.LocalDateTime;

/**
 * Admin dashboard için istatistik DTO
 */
public class DashboardStats {
    
    private long totalTenants;
    private long activeTenants;
    private long totalCustomers;
    private long totalAppointments;
    private long todayAppointments;
    private long monthlyAppointments;
    private LocalDateTime lastUpdated;

    // Constructors
    public DashboardStats() {
        this.lastUpdated = LocalDateTime.now();
    }

    public DashboardStats(long totalTenants, long activeTenants, long totalCustomers, 
                         long totalAppointments, long todayAppointments, long monthlyAppointments) {
        this.totalTenants = totalTenants;
        this.activeTenants = activeTenants;
        this.totalCustomers = totalCustomers;
        this.totalAppointments = totalAppointments;
        this.todayAppointments = todayAppointments;
        this.monthlyAppointments = monthlyAppointments;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public long getTotalTenants() {
        return totalTenants;
    }

    public void setTotalTenants(long totalTenants) {
        this.totalTenants = totalTenants;
    }

    public long getActiveTenants() {
        return activeTenants;
    }

    public void setActiveTenants(long activeTenants) {
        this.activeTenants = activeTenants;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getTodayAppointments() {
        return todayAppointments;
    }

    public void setTodayAppointments(long todayAppointments) {
        this.todayAppointments = todayAppointments;
    }

    public long getMonthlyAppointments() {
        return monthlyAppointments;
    }

    public void setMonthlyAppointments(long monthlyAppointments) {
        this.monthlyAppointments = monthlyAppointments;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "DashboardStats{" +
                "totalTenants=" + totalTenants +
                ", activeTenants=" + activeTenants +
                ", totalCustomers=" + totalCustomers +
                ", totalAppointments=" + totalAppointments +
                ", todayAppointments=" + todayAppointments +
                ", monthlyAppointments=" + monthlyAppointments +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
