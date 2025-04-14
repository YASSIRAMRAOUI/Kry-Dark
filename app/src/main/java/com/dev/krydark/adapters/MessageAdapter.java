package com.dev.krydark.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.dev.krydark.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private Context context;
    private FirebaseAuth mAuth;

    public MessageAdapter(List<Message> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (mAuth.getCurrentUser() == null) {
            return VIEW_TYPE_RECEIVED; // or handle this case appropriately
        }
        String currentUserId = mAuth.getCurrentUser().getUid();

        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Format time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(message.getTimestamp()));

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            SentMessageViewHolder viewHolder = (SentMessageViewHolder) holder;

            // Handle message types
            if (message.isTextMessage()) {
                viewHolder.textMessage.setVisibility(View.VISIBLE);
                viewHolder.imageMessage.setVisibility(View.GONE);
                viewHolder.textMessage.setText(message.getContent());
            } else if (message.isImageMessage()) {
                viewHolder.textMessage.setVisibility(View.GONE);
                viewHolder.imageMessage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getMediaUrl())
                        .into(viewHolder.imageMessage);
            }

            viewHolder.messageTime.setText(time);

            // Set read status
            viewHolder.messageStatus.setVisibility(View.VISIBLE);
            if (message.isRead()) {
                viewHolder.messageStatus.setImageResource(R.drawable.ic_read);
            } else {
                viewHolder.messageStatus.setImageResource(R.drawable.ic_delivered);
            }
        } else {
            ReceivedMessageViewHolder viewHolder = (ReceivedMessageViewHolder) holder;

            // Handle message types
            if (message.isTextMessage()) {
                viewHolder.textMessage.setVisibility(View.VISIBLE);
                viewHolder.imageMessage.setVisibility(View.GONE);
                viewHolder.textMessage.setText(message.getContent());
            } else if (message.isImageMessage()) {
                viewHolder.textMessage.setVisibility(View.GONE);
                viewHolder.imageMessage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getMediaUrl())
                        .into(viewHolder.imageMessage);
            }

            viewHolder.messageTime.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, messageTime;
        ImageView imageMessage, messageStatus;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            messageTime = itemView.findViewById(R.id.message_time);
            imageMessage = itemView.findViewById(R.id.image_message);
            messageStatus = itemView.findViewById(R.id.message_status);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, messageTime;
        ImageView imageMessage, messageStatus; // Add this line

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            messageTime = itemView.findViewById(R.id.message_time);
            imageMessage = itemView.findViewById(R.id.image_message);
            messageStatus = itemView.findViewById(R.id.message_status); // Add this line
        }
    }
}