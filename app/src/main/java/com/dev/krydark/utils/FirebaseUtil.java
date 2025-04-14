package com.dev.krydark.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for common Firebase operations
 */
public class FirebaseUtil {
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static FirebaseStorage storage = FirebaseStorage.getInstance();

    // User methods
    public static String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static void getUserData(String userId, OnUserDataListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        listener.onUserDataReceived(task.getResult());
                    } else {
                        listener.onError(task.getException() != null ?
                                task.getException().getMessage() : "Error fetching user data");
                    }
                });
    }

    public static String getUserType(DocumentSnapshot userDoc) {
        return userDoc.getString("userType");
    }

    public static String getUserDisplayName(DocumentSnapshot userDoc) {
        String userType = getUserType(userDoc);
        if ("agency".equals(userType)) {
            return userDoc.getString("agencyName");
        } else {
            String firstName = userDoc.getString("firstName");
            String lastName = userDoc.getString("lastName");
            return firstName + " " + lastName;
        }
    }

    // Property methods
    public static void addProperty(Map<String, Object> propertyData, OnPropertyAddedListener listener) {
        db.collection("properties")
                .add(propertyData)
                .addOnSuccessListener(documentReference -> {
                    listener.onPropertyAdded(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    public static void getPropertyById(String propertyId, OnPropertyFetchedListener listener) {
        db.collection("properties").document(propertyId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        listener.onPropertyFetched(task.getResult());
                    } else {
                        listener.onError(task.getException() != null ?
                                task.getException().getMessage() : "Property not found");
                    }
                });
    }

    public static void toggleFavorite(String propertyId, OnToggleFavoriteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        DocumentReference favoriteRef = db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(propertyId);

        favoriteRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Remove from favorites
                    favoriteRef.delete()
                            .addOnSuccessListener(aVoid -> listener.onFavoriteToggled(false))
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                } else {
                    // Add to favorites
                    Map<String, Object> favoriteData = new HashMap<>();
                    favoriteData.put("propertyId", propertyId);
                    favoriteData.put("addedAt", System.currentTimeMillis());

                    favoriteRef.set(favoriteData)
                            .addOnSuccessListener(aVoid -> listener.onFavoriteToggled(true))
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                }
            } else {
                listener.onError(task.getException().getMessage());
            }
        });
    }

    public static void checkIfFavorite(String propertyId, OnCheckFavoriteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onResult(false);
            return;
        }

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(propertyId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        listener.onResult(document.exists());
                    } else {
                        listener.onResult(false);
                    }
                });
    }

    // Reservation methods
    public static void createReservation(String propertyId, OnReservationCreatedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            listener.onError("User not logged in");
            return;
        }

        Map<String, Object> reservationData = new HashMap<>();
        reservationData.put("userId", userId);
        reservationData.put("propertyId", propertyId);
        reservationData.put("status", "pending");
        reservationData.put("createdAt", System.currentTimeMillis());

        db.collection("reservations")
                .add(reservationData)
                .addOnSuccessListener(documentReference -> {
                    listener.onReservationCreated(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    // Chat methods
    public static void getOrCreateChat(String recipientId, String propertyId, OnChatCreatedListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            listener.onError("User not logged in");
            return;
        }

        // Check if a chat already exists between these users
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String chatId = null;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<String> participants = (List<String>) document.get("participants");
                            if (participants != null && participants.contains(recipientId)) {
                                String chatPropertyId = document.getString("propertyId");

                                // If no property is specified, or it matches the current property
                                if (propertyId == null || propertyId.equals(chatPropertyId)) {
                                    chatId = document.getId();
                                    break;
                                }
                            }
                        }

                        if (chatId != null) {
                            // Chat already exists, return its ID
                            listener.onChatCreated(chatId);
                        } else {
                            // Create a new chat
                            createNewChat(currentUserId, recipientId, propertyId, listener);
                        }
                    } else {
                        listener.onError(task.getException().getMessage());
                    }
                });
    }

    private static void createNewChat(String currentUserId, String recipientId, String propertyId, OnChatCreatedListener listener) {
        Map<String, Object> chatData = new HashMap<>();
        List<String> participants = new ArrayList<>();
        participants.add(currentUserId);
        participants.add(recipientId);

        chatData.put("participants", participants);
        if (propertyId != null) {
            chatData.put("propertyId", propertyId);
        }
        chatData.put("lastMessage", "");
        chatData.put("lastMessageTime", System.currentTimeMillis());

        Map<String, Integer> unreadCount = new HashMap<>();
        unreadCount.put(currentUserId, 0);
        unreadCount.put(recipientId, 0);
        chatData.put("unreadCount", unreadCount);

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    listener.onChatCreated(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                });
    }

    // Interfaces
    public interface OnUserDataListener {
        void onUserDataReceived(DocumentSnapshot userDoc);
        void onError(String errorMessage);
    }

    public interface OnPropertyAddedListener {
        void onPropertyAdded(String propertyId);
        void onError(String errorMessage);
    }

    public interface OnPropertyFetchedListener {
        void onPropertyFetched(DocumentSnapshot propertyDoc);
        void onError(String errorMessage);
    }

    public interface OnToggleFavoriteListener {
        void onFavoriteToggled(boolean isFavorite);
        void onError(String errorMessage);
    }

    public interface OnCheckFavoriteListener {
        void onResult(boolean isFavorite);
    }

    public interface OnReservationCreatedListener {
        void onReservationCreated(String reservationId);
        void onError(String errorMessage);
    }

    public interface OnChatCreatedListener {
        void onChatCreated(String chatId);
        void onError(String errorMessage);
    }
}