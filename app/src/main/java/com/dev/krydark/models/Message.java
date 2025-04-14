package com.dev.krydark.models;

public class Message {
    private String id;
    private String chatId;
    private String senderId;
    private String content;
    private long timestamp;
    private boolean read;
    private String type; // "text", "image"
    private String mediaUrl; // for images

    // No-argument constructor needed for Firestore
    public Message() {}

    // Constructor for text message
    public Message(String chatId, String senderId, String content) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
        this.type = "text";
    }

    // Constructor for media message
    public Message(String chatId, String senderId, String mediaUrl, String type) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.mediaUrl = mediaUrl;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
        this.type = type;
    }

    // Constructor with all fields
    public Message(String id, String chatId, String senderId, String content,
                   long timestamp, boolean read, String type, String mediaUrl) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
        this.mediaUrl = mediaUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    // Helper methods
    public boolean isTextMessage() {
        return "text".equals(type);
    }

    public boolean isImageMessage() {
        return "image".equals(type);
    }
}