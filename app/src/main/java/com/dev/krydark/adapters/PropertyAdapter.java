package com.dev.krydark.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;
import com.dev.krydark.dialogs.PropertyDetailDialog;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.dev.krydark.models.Property;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {
    private List<Property> propertyList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public PropertyAdapter(List<Property> propertyList, Context context) {
        this.propertyList = propertyList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_property, parent, false);
        return new PropertyViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Property property = propertyList.get(position);

        // Format price with currency
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        holder.propertyTitle.setText(property.getTitle());
        holder.propertyPrice.setText(format.format(property.getPrice()));

        // Set location and size
        holder.propertyLocation.setText(property.getLocation());
        holder.propertySize.setText(property.getSize() + " m²");

        // Load the first image from the list
        if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(property.getImageUrls().get(0))
                    .placeholder(R.drawable.placeholder_property)
                    .into(holder.propertyImage);
        }

        // Check if property is in favorites
        checkIfFavorite(property.getId(), holder.btnFavorite);

        // Set click listeners
        holder.propertyImage.setOnClickListener(v -> showPropertyDetails(property));

        holder.btnFavorite.setOnClickListener(v -> toggleFavorite(property.getId(), holder.btnFavorite));
    }

    @Override
    public int getItemCount() {
        return propertyList.size();
    }

    private void checkIfFavorite(String propertyId, ImageButton favoriteButton) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        favoriteButton.setImageResource(R.drawable.ic_favorite);
                    } else {
                        favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                    }
                });
    }

    private void toggleFavorite(String propertyId, ImageButton favoriteButton) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Remove from favorites
                        db.collection("users")
                                .document(userId)
                                .collection("favorites")
                                .document(propertyId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                                });
                    } else {
                        // Add to favorites
                        db.collection("users")
                                .document(userId)
                                .collection("favorites")
                                .document(propertyId)
                                .set(new HashMap<String, Object>() {{
                                    put("propertyId", propertyId);
                                    put("addedAt", System.currentTimeMillis());
                                }})
                                .addOnSuccessListener(aVoid -> {
                                    favoriteButton.setImageResource(R.drawable.ic_favorite);
                                });
                    }
                });
    }

    private void showPropertyDetails(Property property) {
        // Create and show property detail dialog or navigate to detail activity
        PropertyDetailDialog dialog = new PropertyDetailDialog(context, property);
        dialog.show(((AppCompatActivity)context).getSupportFragmentManager(), "property_detail");
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImage;
        TextView propertyPrice, propertyLocation, propertySize, propertyTitle;
        ImageButton btnFavorite;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImage = itemView.findViewById(R.id.property_image);
            propertyPrice = itemView.findViewById(R.id.property_price);
            propertyTitle = itemView.findViewById(R.id.property_title);
            propertyLocation = itemView.findViewById(R.id.property_location);
            propertySize = itemView.findViewById(R.id.property_size);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}