package com.dev.krydark.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dev.krydark.R;
import com.dev.krydark.adapters.PropertyAdapter;
import com.dev.krydark.models.Property;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import android.util.Log;

import com.google.firebase.firestore.Query;

public class HomeFragment extends Fragment {
    private RecyclerView propertyRecyclerView;
    private PropertyAdapter propertyAdapter;
    private List<Property> propertyList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private SearchView searchView;
    private RangeSlider priceRangeSlider;
    private AutoCompleteTextView locationFilter;
    private Button btnApplyFilter;
    private FloatingActionButton fabAddProperty;

    private float minPrice = 0;
    private float maxPrice = 1000000;
    private String selectedLocation = "";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        propertyList = new ArrayList<>();

        // Initialize views
        searchView = view.findViewById(R.id.search_view);
        priceRangeSlider = view.findViewById(R.id.price_range_slider);
        locationFilter = view.findViewById(R.id.location_filter);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        propertyRecyclerView = view.findViewById(R.id.property_recycler_view);
        fabAddProperty = view.findViewById(R.id.fab_add_property);

        // Setup recycler view
        propertyAdapter = new PropertyAdapter(propertyList, getContext());
        propertyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        propertyRecyclerView.setAdapter(propertyAdapter);

        // Setup location dropdown
        setupLocationDropdown();

        // Setup search view
        setupSearchView();

        // Setup price range slider
        setupPriceRangeSlider();

        // Setup apply button
        btnApplyFilter.setOnClickListener(v -> applyFilters());

        // Setup FAB for agency users
        setupAddPropertyFab();

        // Load initial data
        loadProperties();

        return view;
    }

    private void setupAddPropertyFab() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userType = documentSnapshot.getString("userType");
                            if (userType != null && userType.equals("agency")) {
                                // Show FAB for agency users
                                fabAddProperty.setVisibility(View.VISIBLE);

                                // Set up click listener
                                fabAddProperty.setOnClickListener(v -> {
                                    // Navigate to AddPropertyFragment
                                    if (getActivity() != null) {
                                        getActivity().getSupportFragmentManager().beginTransaction()
                                                .replace(R.id.fragment_container, new AddPropertyFragment())
                                                .addToBackStack(null)
                                                .commit();
                                    }
                                });
                            } else {
                                // Hide FAB for non-agency users
                                fabAddProperty.setVisibility(View.GONE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Hide FAB in case of error
                        fabAddProperty.setVisibility(View.GONE);
                    });
        } else {
            // Hide FAB if user is not logged in
            fabAddProperty.setVisibility(View.GONE);
        }
    }

    private void setupLocationDropdown() {
        // Create a list to store unique locations
        List<String> locations = new ArrayList<>();

        // Fetch unique locations from properties collection
        db.collection("properties")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Collect unique locations from properties
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String location = document.getString("location");
                            if (location != null && !locations.contains(location)) {
                                locations.add(location);
                            }
                        }

                        // Sort locations alphabetically
                        Collections.sort(locations);

                        // Create and set adapter for dropdown
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                locations
                        );
                        locationFilter.setAdapter(adapter);

                        // Set item click listener
                        locationFilter.setOnItemClickListener((parent, view, position, id) -> {
                            selectedLocation = (String) parent.getItemAtPosition(position);
                        });
                    } else {
                        // Handle error in fetching locations
                        Log.e("LocationDropdown", "Error getting locations", task.getException());
                    }
                });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty() && !searchQuery.isEmpty()) {
                    searchQuery = "";
                    applyFilters();
                }
                return true;
            }
        });
    }

    private void setupPriceRangeSlider() {
        // Fetch max property price first
        db.collection("properties")
                .orderBy("price", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    float maxPropertyPrice = 1000000f; // Default fallback

                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        maxPropertyPrice = document.getDouble("price").floatValue();

                        // Set slider max value to max property price
                        priceRangeSlider.setValueTo(maxPropertyPrice);
                        priceRangeSlider.setValues(0f, maxPropertyPrice);
                    }

                    priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
                        List<Float> values = slider.getValues();
                        minPrice = values.get(0);
                        maxPrice = values.get(1);
                    });
                })
                .addOnFailureListener(e -> {
                    // Fallback to default max if query fails
                    priceRangeSlider.setValueTo(1000000f);
                    priceRangeSlider.setValues(0f, 1000000f);
                });
    }

    private void applyFilters() {
        loadProperties();
    }

    private void loadProperties() {
        propertyList.clear();

        Query query = db.collection("properties");

        // Apply price range filter
        query = query.whereGreaterThanOrEqualTo("price", minPrice)
                .whereLessThanOrEqualTo("price", maxPrice);

        // Apply location filter if selected
        if (!selectedLocation.isEmpty()) {
            query = query.whereEqualTo("location", selectedLocation);
        }

        query.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Property property = document.toObject(Property.class);
                            property.setId(document.getId());

                            // Apply search query filter
                            boolean matchesSearch = searchQuery.isEmpty() ||
                                    property.getTitle().toLowerCase().contains(searchQuery.toLowerCase());

                            if (matchesSearch) {
                                propertyList.add(property);
                            }
                        }

                        propertyAdapter.notifyDataSetChanged();
                    }
                });
    }
}