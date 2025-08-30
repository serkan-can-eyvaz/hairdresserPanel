package com.example.barber.automation.dto;

/**
 * Login response DTO
 */
public class LoginResponse {
    
    private String token;
    private String tokenType = "Bearer";
    private String username;
    private String role;
    private Long tenantId;
    private String tenantName;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String token, String username, String role, Long tenantId, String tenantName) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", tenantId=" + tenantId +
                ", tenantName='" + tenantName + '\'' +
                '}';
    }
}
