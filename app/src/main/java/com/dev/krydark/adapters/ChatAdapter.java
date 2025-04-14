package com.dev.krydark.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.dev.krydark.models.Chat;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<Chat> chatList;
    private Context context;
    private OnChatClickListener listener;
    private FirebaseAuth mAuth;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatAdapter(List<Chat> chatList, Context context, OnChatClickListener listener) {
        this.chatList = chatList;
        this.context = context;
        this.listener = listener;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        if (mAuth.getCurrentUser() == null) {
            return; // or handle this case appropriately
        }

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Set user name (with null check)
        String name = chat.getOtherUserName();
        holder.userName.setText(name != null ? name : "Unknown User");

        // Set last message (with null check)
        String lastMsg = chat.getLastMessage();
        holder.lastMessage.setText(lastMsg != null ? lastMsg : "");

        // Set time
        long lastMsgTime = chat.getLastMessageTime();
        if (lastMsgTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = sdf.format(new Date(lastMsgTime));
            holder.messageTime.setText(time);
        } else {
            holder.messageTime.setText("");
        }

        // Set unread count
        int unreadCount = chat.getUnreadCountForUser(currentUserId);
        if (unreadCount > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(unreadCount));
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }

        // Load user image
        String imageUrl = chat.getOtherUserImage();
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_profile)
                .into(holder.userImage);

        // Set property info if applicable
        String propertyTitle = chat.getPropertyTitle();
        if (propertyTitle != null && !propertyTitle.isEmpty()) {
            holder.propertyInfo.setVisibility(View.VISIBLE);
            holder.propertyInfo.setText("RE: " + propertyTitle);
        } else {
            holder.propertyInfo.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userImage;
        TextView userName, lastMessage, messageTime, unreadCount, propertyInfo;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.user_image);
            userName = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            messageTime = itemView.findViewById(R.id.message_time);
            unreadCount = itemView.findViewById(R.id.unread_count);
            propertyInfo = itemView.findViewById(R.id.property_info);
        }
    }
}