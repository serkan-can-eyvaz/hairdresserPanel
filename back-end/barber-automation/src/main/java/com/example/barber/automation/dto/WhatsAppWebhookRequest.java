package com.example.barber.automation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * WhatsApp webhook request DTO
 */
public class WhatsAppWebhookRequest {
    
    @JsonProperty("object")
    private String object;
    
    @JsonProperty("entry")
    private List<Entry> entry;
    
    // Constructors
    public WhatsAppWebhookRequest() {}
    
    // Getters and Setters
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public List<Entry> getEntry() {
        return entry;
    }
    
    public void setEntry(List<Entry> entry) {
        this.entry = entry;
    }
    
    public static class Entry {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("changes")
        private List<Change> changes;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public List<Change> getChanges() {
            return changes;
        }
        
        public void setChanges(List<Change> changes) {
            this.changes = changes;
        }
    }
    
    public static class Change {
        @JsonProperty("value")
        private Value value;
        
        @JsonProperty("field")
        private String field;
        
        public Value getValue() {
            return value;
        }
        
        public void setValue(Value value) {
            this.value = value;
        }
        
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
    }
    
    public static class Value {
        @JsonProperty("messaging_product")
        private String messagingProduct;
        
        @JsonProperty("metadata")
        private Metadata metadata;
        
        @JsonProperty("contacts")
        private List<Contact> contacts;
        
        @JsonProperty("messages")
        private List<Message> messages;
        
        public String getMessagingProduct() {
            return messagingProduct;
        }
        
        public void setMessagingProduct(String messagingProduct) {
            this.messagingProduct = messagingProduct;
        }
        
        public Metadata getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Metadata metadata) {
            this.metadata = metadata;
        }
        
        public List<Contact> getContacts() {
            return contacts;
        }
        
        public void setContacts(List<Contact> contacts) {
            this.contacts = contacts;
        }
        
        public List<Message> getMessages() {
            return messages;
        }
        
        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
    }
    
    public static class Metadata {
        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;
        
        @JsonProperty("phone_number_id")
        private String phoneNumberId;
        
        public String getDisplayPhoneNumber() {
            return displayPhoneNumber;
        }
        
        public void setDisplayPhoneNumber(String displayPhoneNumber) {
            this.displayPhoneNumber = displayPhoneNumber;
        }
        
        public String getPhoneNumberId() {
            return phoneNumberId;
        }
        
        public void setPhoneNumberId(String phoneNumberId) {
            this.phoneNumberId = phoneNumberId;
        }
    }
    
    public static class Contact {
        @JsonProperty("profile")
        private Profile profile;
        
        @JsonProperty("wa_id")
        private String waId;
        
        public Profile getProfile() {
            return profile;
        }
        
        public void setProfile(Profile profile) {
            this.profile = profile;
        }
        
        public String getWaId() {
            return waId;
        }
        
        public void setWaId(String waId) {
            this.waId = waId;
        }
    }
    
    public static class Profile {
        @JsonProperty("name")
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    public static class Message {
        @JsonProperty("from")
        private String from;
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("text")
        private Text text;
        
        @JsonProperty("type")
        private String type;
        
        public String getFrom() {
            return from;
        }
        
        public void setFrom(String from) {
            this.from = from;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
        
        public Text getText() {
            return text;
        }
        
        public void setText(Text text) {
            this.text = text;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
    
    public static class Text {
        @JsonProperty("body")
        private String body;
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
    }
}
