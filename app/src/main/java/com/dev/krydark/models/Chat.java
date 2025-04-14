package com.dev.krydark.models;

import java.util.List;
import java.util.Map;

public class Chat {
    private String id;
    private List<String> participants;
    private String propertyId; // optional, if chat is about a property
    private String lastMessage;
    private long lastMessageTime;

    // For new chats (these will be used instead of the map)
    private Integer unreadCount_currentUser;
    private Integer unreadCount_otherUser;

    // This will be kept for backward compatibility with existing chats
    private Map<String, Integer> unreadCount; // userId -> unread count

    // These fields are not stored in Firestore but used for UI
    private String otherUserName;
    private String otherUserImage;
    private String propertyTitle; // If chat is about a property

    // No-argument constructor needed for Firestore
    public Chat() {}

    // Constructor
    public Chat(String id, List<String> participants, String propertyId,
                String lastMessage, long lastMessageTime) {
        this.id = id;
        this.participants = participants;
        this.propertyId = propertyId;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public Map<String, Integer> getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Map<String, Integer> unreadCount) { this.unreadCount = unreadCount; }

    public Integer getUnreadCount_currentUser() { return unreadCount_currentUser; }
    public void setUnreadCount_currentUser(Integer unreadCount_currentUser) { this.unreadCount_currentUser = unreadCount_currentUser; }

    public Integer getUnreadCount_otherUser() { return unreadCount_otherUser; }
    public void setUnreadCount_otherUser(Integer unreadCount_otherUser) { this.unreadCount_otherUser = unreadCount_otherUser; }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserImage() { return otherUserImage; }
    public void setOtherUserImage(String otherUserImage) { this.otherUserImage = otherUserImage; }

    public String getPropertyTitle() { return propertyTitle; }
    public void setPropertyTitle(String propertyTitle) { this.propertyTitle = propertyTitle; }

    // Helper method to get the other participant's ID
    public String getOtherParticipantId(String currentUserId) {
        if (participants == null) {
            return null;
        }

        for (String participantId : participants) {
            if (participantId != null && !participantId.equals(currentUserId)) {
                return participantId;
            }
        }
        return null;
    }

    // Helper method to get unread count for a user
    public int getUnreadCountForUser(String userId) {
        // Try to get from the individual fields first (new format)
        String fieldName = "unreadCount_" + userId;
        if (fieldName.equals("unreadCount_currentUser") && unreadCount_currentUser != null) {
            return unreadCount_currentUser;
        } else if (fieldName.equals("unreadCount_otherUser") && unreadCount_otherUser != null) {
            return unreadCount_otherUser;
        }

        // Fall back to the map for backward compatibility
        if (unreadCount != null && unreadCount.containsKey(userId)) {
            return unreadCount.get(userId);
        }

        return 0;
    }
}