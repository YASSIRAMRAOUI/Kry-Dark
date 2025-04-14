package com.dev.krydark.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

import com.dev.krydark.R;
import com.dev.krydark.adapters.PropertyImageAdapter;
import com.dev.krydark.fragments.ChatDetailFragment;
import com.dev.krydark.models.Property;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PropertyDetailDialog extends BottomSheetDialogFragment {
    private Property property;
    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public PropertyDetailDialog(Context context, Property property) {
        this.property = property;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.dialog_property_detail, null);
        dialog.setContentView(view);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        ViewPager2 imagesPager = view.findViewById(R.id.property_images_pager);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        TextView propertyTitle = view.findViewById(R.id.property_title);
        TextView propertyPrice = view.findViewById(R.id.property_price);
        TextView propertyLocation = view.findViewById(R.id.property_location);
        TextView propertySize = view.findViewById(R.id.property_size);
        TextView propertyBedrooms = view.findViewById(R.id.property_bedrooms);
        TextView propertyBathrooms = view.findViewById(R.id.property_bathrooms);
        TextView propertyDescription = view.findViewById(R.id.property_description);
        ChipGroup amenitiesChipGroup = view.findViewById(R.id.amenities_chip_group);
        Button btnReserve = view.findViewById(R.id.btn_reserve);

        // Setup image slider
        if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
            PropertyImageAdapter imageAdapter = new PropertyImageAdapter(property.getImageUrls(), context);
            imagesPager.setAdapter(imageAdapter);

            new TabLayoutMediator(tabLayout, imagesPager, (tab, position) -> {
                // No title for the tabs
            }).attach();
        }

        // Set property details
        propertyTitle.setText(property.getTitle());

        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        propertyPrice.setText(format.format(property.getPrice()));

        propertyLocation.setText(property.getLocation());
        propertySize.setText(property.getSize() + " m²");
        propertyBedrooms.setText(property.getBedrooms() + " bedrooms");
        propertyBathrooms.setText(property.getBathrooms() + " bathrooms");
        propertyDescription.setText(property.getDescription());

        // Add amenity chips
        if (property.getAmenities() != null) {
            for (String amenity : property.getAmenities()) {
                Chip chip = new Chip(context);
                chip.setText(amenity);
                amenitiesChipGroup.addView(chip);
            }
        }

        // Set button click listeners
        btnReserve.setOnClickListener(v -> createReservation());

        return dialog;
    }

    private void createReservation() {
        String userId = mAuth.getCurrentUser().getUid();
        String propertyId = property.getId();

        // Check if user already has a reservation for this property
        db.collection("reservations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("propertyId", propertyId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create new reservation
                        Map<String, Object> reservation = new HashMap<>();
                        reservation.put("userId", userId);
                        reservation.put("propertyId", propertyId);
                        reservation.put("status", "pending");
                        reservation.put("createdAt", System.currentTimeMillis());

                        db.collection("reservations")
                                .add(reservation)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(context, "Reservation created successfully", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to create reservation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(context, "You already have a reservation for this property", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}