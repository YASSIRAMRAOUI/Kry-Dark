package com.dev.krydark.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.dev.krydark.dialogs.PropertyDetailDialog;
import com.dev.krydark.models.Property;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {
    private List<Property> favoritesList;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public FavoriteAdapter(List<Property> favoritesList, Context context) {
        this.favoritesList = favoritesList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_property, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Property property = favoritesList.get(position);

        // Format price with currency
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
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

        // Set the favorite button to filled heart
        holder.btnFavorite.setImageResource(R.drawable.ic_favorite);

        // Set click listeners
        holder.propertyImage.setOnClickListener(v -> showPropertyDetails(property));

        holder.btnFavorite.setOnClickListener(v -> removeFromFavorites(property, position));
    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }

    private void removeFromFavorites(Property property, int position) {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .document(property.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from the list and notify the adapter
                    favoritesList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, favoritesList.size());
                });
    }

    private void showPropertyDetails(Property property) {
        PropertyDetailDialog dialog = new PropertyDetailDialog(context, property);
        dialog.show(((androidx.fragment.app.FragmentActivity)context).getSupportFragmentManager(), "PropertyDetailDialog");
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImage;
        TextView propertyPrice, propertyLocation, propertySize;
        ImageButton btnFavorite;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImage = itemView.findViewById(R.id.property_image);
            propertyPrice = itemView.findViewById(R.id.property_price);
            propertyLocation = itemView.findViewById(R.id.property_location);
            propertySize = itemView.findViewById(R.id.property_size);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
        }
    }
}