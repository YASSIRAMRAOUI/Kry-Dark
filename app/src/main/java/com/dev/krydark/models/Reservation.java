package com.dev.krydark.models;

public class Reservation {
    private String id;
    private String userId;
    private String propertyId;
    private String status; // "pending", "confirmed", "cancelled"
    private long createdAt;

    // These fields are not stored in Firestore but used for UI
    private String propertyTitle;
    private String propertyLocation;
    private String propertyImageUrl;
    private String ownerId;
    private String ownerName;
    private String ownerPhone;

    // No-argument constructor needed for Firestore
    public Reservation() {}

    // Constructor for creating a new reservation
    public Reservation(String userId, String propertyId, String status) {
        this.userId = userId;
        this.propertyId = propertyId;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor with all fields (including UI fields)
    public Reservation(String id, String userId, String propertyId, String status, long createdAt,
                       String propertyTitle, String propertyLocation, String propertyImageUrl,
                       String ownerId, String ownerName, String ownerPhone) {
        this.id = id;
        this.userId = userId;
        this.propertyId = propertyId;
        this.status = status;
        this.createdAt = createdAt;
        this.propertyTitle = propertyTitle;
        this.propertyLocation = propertyLocation;
        this.propertyImageUrl = propertyImageUrl;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerPhone = ownerPhone;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }

    public String getPropertyLocation() { return propertyLocation; }
    public void setPropertyLocation(String propertyLocation) { this.propertyLocation = propertyLocation; }

    public String getPropertyImageUrl() { return propertyImageUrl; }
    public void setPropertyImageUrl(String propertyImageUrl) { this.propertyImageUrl = propertyImageUrl; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
}