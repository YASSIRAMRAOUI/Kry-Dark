package com.dev.krydark.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dev.krydark.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AddPropertyFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGES = 5;

    private TextInputEditText etTitle, etDescription, etPrice, etLocation, etSize;
    private Spinner spinnerBedrooms, spinnerBathrooms;
    private ChipGroup amenitiesChipGroup;
    private LinearLayout imagesContainer;
    private Button btnAddImage, btnAddProperty;
    private ProgressBar progressBar;

    private List<Uri> imageUris = new ArrayList<>();
    private List<String> amenitiesList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private int selectedBedrooms = 0;
    private int selectedBathrooms = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_property, container, false);

        // Initialize Firebase components
        initializeFirebaseComponents();

        // Initialize UI components
        initializeUIComponents(view);

        // Setup spinners and amenities
        setupSpinners();
        setupAmenitiesChips();

        // Set click listeners
        setupClickListeners();

        return view;
    }

    private void initializeFirebaseComponents() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("property_images");
    }

    private void initializeUIComponents(View view) {
        etTitle = view.findViewById(R.id.et_title);
        etDescription = view.findViewById(R.id.et_description);
        etPrice = view.findViewById(R.id.et_price);
        etLocation = view.findViewById(R.id.et_location);
        etSize = view.findViewById(R.id.et_size);
        spinnerBedrooms = view.findViewById(R.id.spinner_bedrooms);
        spinnerBathrooms = view.findViewById(R.id.spinner_bathrooms);
        amenitiesChipGroup = view.findViewById(R.id.amenities_chip_group);
        imagesContainer = view.findViewById(R.id.images_container);
        btnAddImage = view.findViewById(R.id.btn_add_image);
        btnAddProperty = view.findViewById(R.id.btn_add_property);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupSpinners() {
        // Bedrooms spinner
        Integer[] bedroomOptions = {0, 1, 2, 3, 4, 5, 6};
        ArrayAdapter<Integer> bedroomsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                bedroomOptions
        );
        bedroomsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBedrooms.setAdapter(bedroomsAdapter);
        spinnerBedrooms.setSelection(0);  // Default to 0

        // Bathrooms spinner
        Integer[] bathroomOptions = {0, 1, 2, 3, 4, 5, 6};
        ArrayAdapter<Integer> bathroomsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                bathroomOptions
        );
        bathroomsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBathrooms.setAdapter(bathroomsAdapter);
        spinnerBathrooms.setSelection(0);  // Default to 0

        // Set listeners to track selected values
        spinnerBedrooms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBedrooms = (int) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBedrooms = 0;
            }
        });

        spinnerBathrooms.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBathrooms = (int) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBathrooms = 0;
            }
        });
    }

    private void setupAmenitiesChips() {
        String[] amenities = {
                "WiFi", "Air Conditioning", "Heating", "Kitchen", "TV", "Parking",
                "Elevator", "Pool", "Gym", "Balcony", "Garden", "Security"
        };

        for (String amenity : amenities) {
            Chip chip = new Chip(requireContext());
            chip.setText(amenity);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    amenitiesList.add(amenity);
                } else {
                    amenitiesList.remove(amenity);
                }
            });
            amenitiesChipGroup.addView(chip);
        }
    }

    private void setupClickListeners() {
        btnAddImage.setOnClickListener(v -> {
            if (imageUris.size() >= MAX_IMAGES) {
                Toast.makeText(requireContext(), "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
                return;
            }
            openFileChooser();
        });

        btnAddProperty.setOnClickListener(v -> validateAndAddProperty());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Check if multiple images are selected or a single image
                if (data.getClipData() != null) {
                    // Multiple images selected
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        if (imageUris.size() < MAX_IMAGES) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            addImageToPreview(imageUri);
                        } else {
                            break;
                        }
                    }
                } else if (data.getData() != null) {
                    // Single image selected
                    Uri imageUri = data.getData();
                    addImageToPreview(imageUri);
                }
            }
        }
    }

    private void addImageToPreview(Uri imageUri) {
        if (imageUris.contains(imageUri)) {
            Toast.makeText(requireContext(), "Image already added", Toast.LENGTH_SHORT).show();
            return;
        }

        imageUris.add(imageUri);

        // Add image preview to the container
        View imageView = LayoutInflater.from(requireContext()).inflate(R.layout.item_property_image_preview, imagesContainer, false);
        ImageView preview = imageView.findViewById(R.id.image_preview);
        ImageButton btnRemove = imageView.findViewById(R.id.btn_remove);

        preview.setImageURI(imageUri);

        // Set tag to identify the image later
        int imageIndex = imageUris.size() - 1;
        imageView.setTag(imageIndex);

        // Set remove button click listener
        btnRemove.setOnClickListener(v -> {
            int index = (int) imageView.getTag();
            imageUris.remove(index);
            imagesContainer.removeView(imageView);

            // Update tags for remaining images
            for (int i = 0; i < imagesContainer.getChildCount(); i++) {
                View child = imagesContainer.getChildAt(i);
                if (child.getTag() != null && (int) child.getTag() > index) {
                    child.setTag((int) child.getTag() - 1);
                }
            }
        });

        imagesContainer.addView(imageView);
    }

    private void validateAndAddProperty() {
        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to add a property", Toast.LENGTH_SHORT).show();
            return;
        }

        // Trim and get values
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String sizeStr = etSize.getText().toString().trim();

        // Reset any previous errors
        resetErrors();

        // Validate input
        if (!validateInputFields(title, description, priceStr, location, sizeStr)) {
            return;
        }

        // Parse numeric values safely
        double price;
        int size;
        try {
            price = Double.parseDouble(priceStr);
            size = Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid price or size", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate images
        if (imageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnAddProperty.setEnabled(false);

        // Upload images first
        uploadImages(currentUser.getUid(), title, description, price, location, size,
                selectedBedrooms, selectedBathrooms);
    }

    private void resetErrors() {
        etTitle.setError(null);
        etDescription.setError(null);
        etPrice.setError(null);
        etLocation.setError(null);
        etSize.setError(null);
    }

    private boolean validateInputFields(String title, String description,
                                        String priceStr, String location, String sizeStr) {
        boolean isValid = true;

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Price is required");
            etPrice.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(location)) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(sizeStr)) {
            etSize.setError("Size is required");
            etSize.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void uploadImages(String ownerId, String title, String description,
                              double price, String location, int size,
                              int bedrooms, int bathrooms) {
        List<String> imageUrls = new ArrayList<>();

        // Generate a unique ID for the property
        String propertyId = db.collection("properties").document().getId();

        // Use AtomicInteger to track upload progress
        AtomicInteger uploadCounter = new AtomicInteger(0);

        // Upload each image
        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);

            String filename = System.currentTimeMillis() + "_" + i + ".jpg";
            StorageReference fileReference = storageRef.child(propertyId).child(filename);

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            synchronized (imageUrls) {
                                imageUrls.add(uri.toString());
                            }

                            // Check if all images are uploaded
                            if (uploadCounter.incrementAndGet() == imageUris.size()) {
                                createProperty(propertyId, ownerId, title, description, price,
                                        location, size, bedrooms, bathrooms, imageUrls);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Handle individual image upload failure
                        Toast.makeText(requireContext(),
                                "Failed to upload image: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // If all uploads fail, reset progress
                        if (uploadCounter.incrementAndGet() == imageUris.size()) {
                            resetProgressState();
                        }
                    });
        }
    }

    private void createProperty(String propertyId, String ownerId, String title,
                                String description, double price, String location,
                                int size, int bedrooms, int bathrooms,
                                List<String> imageUrls) {
        Map<String, Object> propertyData = new HashMap<>();
        propertyData.put("ownerId", ownerId);
        propertyData.put("title", title);
        propertyData.put("description", description);
        propertyData.put("price", price);
        propertyData.put("location", location);
        propertyData.put("size", size);
        propertyData.put("bedrooms", bedrooms);
        propertyData.put("bathrooms", bathrooms);
        propertyData.put("amenities", amenitiesList);
        propertyData.put("imageUrls", imageUrls);
        propertyData.put("createdAt", System.currentTimeMillis());
        propertyData.put("status", "active"); // Default status

        db.collection("properties").document(propertyId)
                .set(propertyData)
                .addOnSuccessListener(aVoid -> {
                    // Property added successfully
                    resetProgressState();

                    // Show success message
                    Toast.makeText(requireContext(),
                            "Property added successfully",
                            Toast.LENGTH_SHORT).show();

                    // Navigate back to home or properties list
                    navigateToHomeFragment();
                })
                .addOnFailureListener(e -> {
                    // Handle property creation failure
                    resetProgressState();

                    Toast.makeText(requireContext(),
                            "Failed to add property: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetProgressState() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnAddProperty.setEnabled(true);

                // Clear input fields
                etTitle.setText("");
                etDescription.setText("");
                etPrice.setText("");
                etLocation.setText("");
                etSize.setText("");

                // Reset spinners
                spinnerBedrooms.setSelection(0);
                spinnerBathrooms.setSelection(0);

                // Clear amenities
                for (int i = 0; i < amenitiesChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) amenitiesChipGroup.getChildAt(i);
                    chip.setChecked(false);
                }
                amenitiesList.clear();

                // Clear images
                imageUris.clear();
                imagesContainer.removeAllViews();
            });
        }
    }

    private void navigateToHomeFragment() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Replace current fragment with HomeFragment
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up resources
        imageUris.clear();
        amenitiesList.clear();
    }
}