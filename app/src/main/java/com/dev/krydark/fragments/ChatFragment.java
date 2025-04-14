package com.dev.krydark.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dev.krydark.R;
import com.dev.krydark.adapters.ChatAdapter;
import com.dev.krydark.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment implements ChatAdapter.OnChatClickListener {
    private RecyclerView chatRecyclerView;
    private TextView emptyView;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        chatList = new ArrayList<>();

        // Initialize views
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        // Setup recycler view
        chatAdapter = new ChatAdapter(chatList, getContext(), this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        // Load chats
        loadChats();

        return view;
    }

    private void loadChats() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        emptyView.setVisibility(View.GONE);
                        chatRecyclerView.setVisibility(View.VISIBLE);

                        // Clear previous list
                        chatList.clear();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Chat chat = document.toObject(Chat.class);
                            chat.setId(document.getId());

                            // Get the other participant's ID
                            String otherUserId = chat.getOtherParticipantId(currentUserId);

                            // Skip invalid chats
                            if (otherUserId == null) {
                                continue;
                            }

                            // Get other user details
                            db.collection("users")
                                    .document(otherUserId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            // Set other user details for UI
                                            if ("agency".equals(userDoc.getString("userType"))) {
                                                chat.setOtherUserName(userDoc.getString("agencyName"));
                                            } else {
                                                String firstName = userDoc.getString("firstName");
                                                String lastName = userDoc.getString("lastName");
                                                if (firstName != null && lastName != null) {
                                                    chat.setOtherUserName(firstName + " " + lastName);
                                                } else if (firstName != null) {
                                                    chat.setOtherUserName(firstName);
                                                } else if (lastName != null) {
                                                    chat.setOtherUserName(lastName);
                                                } else {
                                                    chat.setOtherUserName("Unknown User");
                                                }
                                            }

                                            chat.setOtherUserImage(userDoc.getString("profileImageUrl"));

                                            // If chat is about a property, get property details
                                            if (chat.getPropertyId() != null && !chat.getPropertyId().isEmpty()) {
                                                db.collection("properties")
                                                        .document(chat.getPropertyId())
                                                        .get()
                                                        .addOnSuccessListener(propertyDoc -> {
                                                            if (propertyDoc.exists()) {
                                                                chat.setPropertyTitle(propertyDoc.getString("title"));
                                                                chatAdapter.notifyDataSetChanged();
                                                            }
                                                        });
                                            }

                                            chatList.add(chat);
                                            chatAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                        chatRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onChatClick(Chat chat) {
        // Make sure we have a valid chat and other user ID
        if (chat == null) {
            return;
        }

        String otherUserId = chat.getOtherParticipantId(mAuth.getCurrentUser().getUid());
        if (otherUserId == null) {
            return;
        }

        // Navigate to chat detail fragment
        Bundle args = new Bundle();
        args.putString("chatId", chat.getId());
        args.putString("otherUserId", otherUserId);
        args.putString("otherUserName", chat.getOtherUserName());
        args.putString("otherUserImage", chat.getOtherUserImage());
        if (chat.getPropertyId() != null) {
            args.putString("propertyId", chat.getPropertyId());
            args.putString("propertyTitle", chat.getPropertyTitle());
        }

        ChatDetailFragment chatDetailFragment = new ChatDetailFragment();
        chatDetailFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, chatDetailFragment)
                .addToBackStack(null)
                .commit();
    }
}