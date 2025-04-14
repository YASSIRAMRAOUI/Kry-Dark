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
import com.dev.krydark.adapters.PropertyAdapter;
import com.dev.krydark.models.Property;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {
    private RecyclerView favoritesRecyclerView;
    private TextView emptyView;
    private PropertyAdapter propertyAdapter;
    private List<Property> favoritesList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        favoritesList = new ArrayList<>();

        // Initialize views
        favoritesRecyclerView = view.findViewById(R.id.favorites_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        // Setup recycler view
        propertyAdapter = new PropertyAdapter(favoritesList, getContext());
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favoritesRecyclerView.setAdapter(propertyAdapter);

        // Load favorite properties
        loadFavorites();

        return view;
    }

    private void loadFavorites() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            favoritesRecyclerView.setVisibility(View.GONE);
                        } else {
                            emptyView.setVisibility(View.GONE);
                            favoritesRecyclerView.setVisibility(View.VISIBLE);

                            // Clear previous list
                            favoritesList.clear();

                            // For each favorite, get the property details
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String propertyId = document.getString("propertyId");

                                if (propertyId != null) {
                                    // Get property details from properties collection
                                    db.collection("properties")
                                            .document(propertyId)
                                            .get()
                                            .addOnSuccessListener(propertyDoc -> {
                                                if (propertyDoc.exists()) {
                                                    Property property = propertyDoc.toObject(Property.class);
                                                    property.setId(propertyDoc.getId());
                                                    favoritesList.add(property);
                                                    propertyAdapter.notifyDataSetChanged();
                                                }
                                            });
                                }
                            }
                        }
                    }
                });
    }
}