package com.example.barber.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class AgentRespondResponse {
    
    private boolean ok;
    private String intent;
    private String reply;
    
    @JsonProperty("next_state")
    private String nextState;
    
    @JsonProperty("extracted_info")
    private Map<String, Object> extractedInfo;
    
    // Constructors
    public AgentRespondResponse() {}
    
    public AgentRespondResponse(boolean ok, String intent, String reply) {
        this.ok = ok;
        this.intent = intent;
        this.reply = reply;
    }
    
    public AgentRespondResponse(boolean ok, String intent, String reply, String nextState, Map<String, Object> extractedInfo) {
        this.ok = ok;
        this.intent = intent;
        this.reply = reply;
        this.nextState = nextState;
        this.extractedInfo = extractedInfo;
    }
    
    // Getters and Setters
    public boolean isOk() {
        return ok;
    }
    
    public void setOk(boolean ok) {
        this.ok = ok;
    }
    
    public String getIntent() {
        return intent;
    }
    
    public void setIntent(String intent) {
        this.intent = intent;
    }
    
    public String getReply() {
        return reply;
    }
    
    public void setReply(String reply) {
        this.reply = reply;
    }
    
    public String getNextState() {
        return nextState;
    }
    
    public void setNextState(String nextState) {
        this.nextState = nextState;
    }
    
    public Map<String, Object> getExtractedInfo() {
        return extractedInfo;
    }
    
    public void setExtractedInfo(Map<String, Object> extractedInfo) {
        this.extractedInfo = extractedInfo;
    }
    
    // Alias for getReply() to match the expected method name
    public String getResponse() {
        return reply;
    }
    
    public void setResponse(String response) {
        this.reply = response;
    }
}


