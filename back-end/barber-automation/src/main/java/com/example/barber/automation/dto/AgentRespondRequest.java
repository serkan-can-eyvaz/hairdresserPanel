package com.example.barber.automation.dto;

public class AgentRespondRequest {
    
    private Long tenant_id;
    private String from_number;
    private String message;
    private String sessionId;
    private String currentState;
    private Long customerId;
    
    // Constructors
    public AgentRespondRequest() {}
    
    public AgentRespondRequest(Long tenant_id, String from_number, String message) {
        this.tenant_id = tenant_id;
        this.from_number = from_number;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getTenant_id() {
        return tenant_id;
    }
    
    public void setTenant_id(Long tenant_id) {
        this.tenant_id = tenant_id;
    }
    
    public String getFrom_number() {
        return from_number;
    }
    
    public void setFrom_number(String from_number) {
        this.from_number = from_number;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getCurrentState() {
        return currentState;
    }
    
    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
    
    public Long getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
}


