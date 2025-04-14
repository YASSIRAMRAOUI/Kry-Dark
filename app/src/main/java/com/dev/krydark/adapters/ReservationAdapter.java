package com.dev.krydark.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.dev.krydark.models.Reservation;

import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {
    private List<Reservation> reservationList;
    private Context context;
    private ReservationClickListener listener;

    public interface ReservationClickListener {
        void onCallClick(String phoneNumber);
        void onMessageClick(String ownerId, String propertyId);
        void onDeleteClick(String reservationId, int position);
    }

    public ReservationAdapter(List<Reservation> reservationList, Context context, ReservationClickListener listener) {
        this.reservationList = reservationList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation reservation = reservationList.get(position);

        // Set property details
        holder.propertyTitle.setText(reservation.getPropertyTitle());
        holder.propertyLocation.setText(reservation.getPropertyLocation());

        // Set status with color based on status value
        holder.reservationStatus.setText(reservation.getStatus().substring(0, 1).toUpperCase() + reservation.getStatus().substring(1));

        switch (reservation.getStatus()) {
            case "pending":
                holder.reservationStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.status_pending));
                break;
            case "confirmed":
                holder.reservationStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.status_confirmed));
                break;
            case "cancelled":
                holder.reservationStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.status_cancelled));
                break;
        }

        // Load property image
        if (reservation.getPropertyImageUrl() != null) {
            Glide.with(context)
                    .load(reservation.getPropertyImageUrl())
                    .placeholder(R.drawable.placeholder_property)
                    .into(holder.propertyImage);
        }

        // Set click listeners
        holder.btnCall.setOnClickListener(v -> {
            if (listener != null && reservation.getOwnerPhone() != null) {
                listener.onCallClick(reservation.getOwnerPhone());
            }
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(reservation.getOwnerId(), reservation.getPropertyId());
            }
        });

        // Set click listener for delete icon
        holder.deleteIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(reservation.getId(), holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return reservationList.size();
    }

    // Method to remove item after deletion
    public void removeItem(int position) {
        reservationList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, reservationList.size());
    }

    static class ReservationViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImage;
        TextView propertyTitle, propertyLocation, reservationStatus;
        Button btnCall, btnMessage;
        ImageView deleteIcon;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImage = itemView.findViewById(R.id.property_image);
            propertyTitle = itemView.findViewById(R.id.property_title);
            propertyLocation = itemView.findViewById(R.id.property_location);
            reservationStatus = itemView.findViewById(R.id.reservation_status);
            btnCall = itemView.findViewById(R.id.btn_call);
            btnMessage = itemView.findViewById(R.id.btn_message);
            deleteIcon = itemView.findViewById(R.id.delete_reservation);
        }
    }
}