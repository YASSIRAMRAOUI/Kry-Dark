package com.dev.krydark.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.dev.krydark.adapters.MessageAdapter;
import com.dev.krydark.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatDetailFragment extends Fragment {
    private static final String TAG = "ChatDetailFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    private RecyclerView messagesRecyclerView;
    private CircleImageView userImage;
    private TextView userName, propertyTitle;
    private EditText messageInput;
    private ImageButton btnSend, btnAttach;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserImage;
    private String propertyId;
    private String currentUserId;

    // Flag to track if a new chat is being created
    private boolean isCreatingChat = false;
    // Pending message to send after chat creation
    private String pendingMessage = null;
    // Flag to indicate if initialization was attempted
    private boolean initializationAttempted = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_detail, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "You must be logged in to chat", Toast.LENGTH_SHORT).show();
            return view;
        }

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("chat_images");
        currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Current user ID: " + currentUserId);

        // Initialize UI components
        initViews(view);
        setupRecyclerView(view);

        // Process arguments and initialize chat
        if (!processArguments()) {
            // Failed to process arguments properly
            return view;
        }

        // Setup click listeners
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
        userImage = view.findViewById(R.id.user_image);
        userName = view.findViewById(R.id.user_name);
        propertyTitle = view.findViewById(R.id.property_title);
        messageInput = view.findViewById(R.id.message_input);
        btnSend = view.findViewById(R.id.btn_send);
        btnAttach = view.findViewById(R.id.btn_attach);
    }

    private void setupRecyclerView(View view) {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private boolean processArguments() {
        Bundle args = getArguments();
        if (args == null) {
            Log.e(TAG, "No arguments provided to ChatDetailFragment");
            Toast.makeText(getContext(), "Error: Chat could not be initialized", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Extract arguments
        chatId = args.getString("chatId");

        // Check for otherUserId, but also accept recipientId for backwards compatibility
        otherUserId = args.getString("otherUserId");
        if (otherUserId == null) {
            otherUserId = args.getString("recipientId");
        }

        otherUserName = args.getString("otherUserName");
        otherUserImage = args.getString("otherUserImage");
        propertyId = args.getString("propertyId");
        String propertyTitleText = args.getString("propertyTitle");

        Log.d(TAG, "Arguments - chatId: " + chatId + ", otherUserId: " + otherUserId);

        // Validate necessary arguments
        if (otherUserId == null) {
            Log.e(TAG, "No otherUserId provided");
            Toast.makeText(getContext(), "Error: No recipient selected", Toast.LENGTH_SHORT).show();
            return false;
        }

        // If otherUserName is not provided, fetch it from Firestore
        if (otherUserName == null && otherUserId != null) {
            fetchOtherUserInfo();
        } else {
            // Set user details with what we have
            updateUserInterface(otherUserName, otherUserImage, propertyTitleText);
        }

        // Initialize the chat
        initializeChat();
        return true;
    }

    private void fetchOtherUserInfo() {
        // Fetch user information from Firestore
        db.collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get user info
                        String userName;
                        String userImage = documentSnapshot.getString("profileImageUrl");

                        if ("agency".equals(documentSnapshot.getString("userType"))) {
                            userName = documentSnapshot.getString("agencyName");
                        } else {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            userName = (firstName != null ? firstName : "") + " " +
                                    (lastName != null ? lastName : "");
                        }

                        otherUserName = userName;
                        otherUserImage = userImage;

                        // If we have a propertyId, also fetch the property title
                        if (propertyId != null) {
                            fetchPropertyInfo();
                        } else {
                            // Update UI with just the user info
                            updateUserInterface(otherUserName, otherUserImage, null);
                        }
                    } else {
                        // If user document doesn't exist, still proceed with chat but show generic info
                        updateUserInterface("User", null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data: " + e.getMessage());
                    // Proceed with chat anyway, but show generic info
                    updateUserInterface("User", null, null);
                });
    }

    private void fetchPropertyInfo() {
        db.collection("properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(propertyDoc -> {
                    String title = "";
                    if (propertyDoc.exists()) {
                        title = propertyDoc.getString("title");
                    }

                    // Now update the UI with all information
                    updateUserInterface(otherUserName, otherUserImage, title);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching property data: " + e.getMessage());
                    // Update UI without property title
                    updateUserInterface(otherUserName, otherUserImage, null);
                });
    }

    private void fetchUserDetails() {
        if (otherUserId == null) {
            return;
        }

        db.collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Determine user type and get appropriate name
                        String userType = documentSnapshot.getString("userType");
                        String name;

                        if ("agency".equals(userType)) {
                            name = documentSnapshot.getString("agencyName");
                        } else {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            name = (firstName != null ? firstName : "") + " " +
                                    (lastName != null ? lastName : "");
                        }

                        // Get profile image URL
                        String imageUrl = documentSnapshot.getString("profileImageUrl");

                        // Update UI with fetched details
                        otherUserName = name;
                        otherUserImage = imageUrl;

                        // Get property title if property ID exists
                        if (propertyId != null) {
                            fetchPropertyTitle();
                        } else {
                            // Update UI without property title
                            updateUserInterface(otherUserName, otherUserImage, null);
                        }
                    } else {
                        // Document doesn't exist, use default values
                        updateUserInterface("User", null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user details: " + e.getMessage());
                    // Use default values on failure
                    updateUserInterface("User", null, null);
                });
    }

    private void fetchPropertyTitle() {
        if (propertyId == null) {
            updateUserInterface(otherUserName, otherUserImage, null);
            return;
        }

        db.collection("properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        updateUserInterface(otherUserName, otherUserImage, title);
                    } else {
                        updateUserInterface(otherUserName, otherUserImage, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching property title: " + e.getMessage());
                    updateUserInterface(otherUserName, otherUserImage, null);
                });
    }

    private void updateUserInterface(String name, String imageUrl, String propTitle) {
        // Update user name
        userName.setText(name != null && !name.trim().isEmpty() ? name : "User");

        // Update user image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_profile)
                    .into(userImage);
        } else {
            // Set default image
            userImage.setImageResource(R.drawable.placeholder_profile);
        }

        // Update property title
        if (propertyId != null && propTitle != null && !propTitle.isEmpty()) {
            propertyTitle.setVisibility(View.VISIBLE);
            propertyTitle.setText("RE: " + propTitle);
        } else {
            propertyTitle.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(messageText)) {
                if (chatId != null) {
                    sendMessage(messageText);
                } else if (isCreatingChat) {
                    // Store the message to be sent after chat creation
                    pendingMessage = messageText;
                    Toast.makeText(getContext(), "Message will be sent when chat is ready", Toast.LENGTH_SHORT).show();
                } else if (otherUserId != null && !initializationAttempted) {
                    // Try to initialize chat again
                    pendingMessage = messageText;
                    initializeChat();
                } else {
                    Toast.makeText(getContext(), "Cannot send message: Chat not initialized", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAttach.setOnClickListener(v -> openFileChooser());
    }

    private void initializeChat() {
        initializationAttempted = true;

        if (chatId != null) {
            // We already have a chat ID, so load messages
            loadMessages();
            return;
        }

        if (otherUserId == null) {
            Log.e(TAG, "Cannot initialize chat: No recipient selected");
            Toast.makeText(getContext(), "Error: No recipient selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // First check if a chat already exists between these users
        findExistingChat();
    }

    private void findExistingChat() {
        Log.d(TAG, "Finding existing chat with user: " + otherUserId);

        // Use a direct query to find chats containing both users
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean chatFound = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        List<String> participants = (List<String>) document.get("participants");
                        if (participants != null && participants.contains(otherUserId)) {
                            chatId = document.getId();
                            Log.d(TAG, "Existing chat found: " + chatId);
                            chatFound = true;

                            // Load messages for this existing chat
                            loadMessages();
                            break;
                        }
                    }

                    if (!chatFound) {
                        // No existing chat found, create a new one
                        Log.d(TAG, "No existing chat found, creating new chat");
                        createNewChat();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding existing chat: " + e.getMessage());
                    // Create a new chat anyway
                    createNewChat();
                });
    }

    private void createNewChat() {
        if (isCreatingChat) {
            // Already creating a chat
            return;
        }

        Log.d(TAG, "Creating new chat with user: " + otherUserId);
        isCreatingChat = true;
        Toast.makeText(getContext(), "Creating new conversation...", Toast.LENGTH_SHORT).show();

        // Create a new chat document
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("participants", Arrays.asList(currentUserId, otherUserId));
        if (propertyId != null) {
            chatData.put("propertyId", propertyId);
        }
        chatData.put("lastMessage", "");
        chatData.put("lastMessageTime", System.currentTimeMillis());

        // Use direct fields for unread counts
        chatData.put("unreadCount_" + currentUserId, 0);
        chatData.put("unreadCount_" + otherUserId, 0);

        db.collection("chats")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    chatId = documentReference.getId();
                    isCreatingChat = false;
                    Log.d(TAG, "New chat created: " + chatId);
                    Toast.makeText(getContext(), "Conversation created", Toast.LENGTH_SHORT).show();

                    // If there's a pending message to send
                    if (pendingMessage != null) {
                        sendMessage(pendingMessage);
                        pendingMessage = null;
                    }
                })
                .addOnFailureListener(e -> {
                    isCreatingChat = false;
                    Log.e(TAG, "Failed to create chat: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to create conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        if (chatId == null) {
            Log.e(TAG, "Cannot load messages: chatId is null");
            return;
        }

        Log.d(TAG, "Loading messages for chat: " + chatId);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for messages: " + e.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                message.setId(dc.getDocument().getId());
                                messageList.add(message);
                            }
                        }

                        messageAdapter.notifyDataSetChanged();
                        if (messageList.size() > 0) {
                            messagesRecyclerView.smoothScrollToPosition(messageList.size() - 1);

                            // Mark messages as read
                            markMessagesAsRead();
                        }
                    }
                });
    }

    private void markMessagesAsRead() {
        if (chatId == null || otherUserId == null) {
            return;
        }

        // Update the direct field for unread count for current user
        DocumentReference chatRef = db.collection("chats").document(chatId);
        chatRef.update("unreadCount_" + currentUserId, 0)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating unread count: " + e.getMessage()));

        // Mark all messages from the other user as read
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", otherUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().update("read", true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error marking messages as read: " + e.getMessage()));
    }

    private void sendMessage(String messageText) {
        // Check if message is empty
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        // Make sure chatId is not null
        if (chatId == null) {
            Log.e(TAG, "Cannot send message: chatId is null");

            if (!initializationAttempted) {
                // Try to initialize chat again
                pendingMessage = messageText;
                initializeChat();
                return;
            }

            if (otherUserId != null && !isCreatingChat) {
                // Try to create a new chat
                pendingMessage = messageText;
                createNewChat();
                return;
            }

            Toast.makeText(getContext(), "Cannot send message: Chat not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Sending message in chat: " + chatId);

        // Create message object
        Message message = new Message(chatId, currentUserId, messageText);

        // Add message to subcollection
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    // Clear input
                    messageInput.setText("");
                    Log.d(TAG, "Message sent successfully");

                    // Update chat document with last message
                    updateChatDocument(messageText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openFileChooser() {
        // Make sure otherUserId is not null
        if (otherUserId == null) {
            Toast.makeText(getContext(), "Cannot upload: No recipient selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure a chat exists before allowing file uploads
        if (chatId == null) {
            if (isCreatingChat) {
                Toast.makeText(getContext(), "Please wait while creating conversation...", Toast.LENGTH_SHORT).show();
            } else if (!initializationAttempted) {
                // Try to initialize chat first
                Toast.makeText(getContext(), "Initializing conversation before uploading...", Toast.LENGTH_SHORT).show();
                initializeChat();
            } else {
                // Need to create a chat first
                Toast.makeText(getContext(), "Creating conversation before uploading...", Toast.LENGTH_SHORT).show();
                createNewChat();
            }
            return;
        }

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri imageUri) {
        // Check if chatId is null
        if (chatId == null) {
            Toast.makeText(getContext(), "Cannot upload: Chat not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress indicator or toast
        Toast.makeText(getContext(), "Uploading image...", Toast.LENGTH_SHORT).show();

        // Generate unique filename
        String filename = System.currentTimeMillis() + ".jpg";
        StorageReference fileReference = storageRef.child(chatId).child(filename);

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Create image message
                        Message message = new Message(chatId, currentUserId, uri.toString(), "image");

                        // Add message to subcollection
                        db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .add(message)
                                .addOnSuccessListener(documentReference -> {
                                    // Update chat document with last message
                                    updateChatDocument("[Image]");
                                    Toast.makeText(getContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save image message: " + e.getMessage());
                                    Toast.makeText(getContext(), "Failed to save message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatDocument(String lastMessage) {
        // Check if chatId is null
        if (chatId == null || otherUserId == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTime", System.currentTimeMillis());

        // Increment the unread count field for the other user
        DocumentReference chatRef = db.collection("chats").document(chatId);
        chatRef.get().addOnSuccessListener(chatDoc -> {
                    if (chatDoc.exists()) {
                        // Get the current unread count for the other user
                        Long currentUnreadCount = chatDoc.getLong("unreadCount_" + otherUserId);
                        if (currentUnreadCount == null) {
                            currentUnreadCount = 0L;
                        }

                        // Increment and update
                        updates.put("unreadCount_" + otherUserId, currentUnreadCount + 1);
                        chatRef.update(updates)
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating chat document: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting chat document: " + e.getMessage()));
    }
}