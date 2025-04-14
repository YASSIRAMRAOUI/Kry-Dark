package com.dev.krydark.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.dev.krydark.activities.LoginActivity;
import com.dev.krydark.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView profileImage;
    private TextView userName, userEmail, userType;
    private TextInputEditText etFirstName, etLastName, etPhone;
    private TextInputEditText etAddress, etCity, etZipCode, etCountry;
    private TextInputEditText etAgencyName, etRegistrationNumber, etWebsite, etDescription;
    private CardView cardAgencyInfo;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private Uri imageUri;
    private String userTypeValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        ImageButton btnChangePhoto = view.findViewById(R.id.btn_change_photo);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        userType = view.findViewById(R.id.user_type);

        etFirstName = view.findViewById(R.id.et_first_name);
        etLastName = view.findViewById(R.id.et_last_name);
        etPhone = view.findViewById(R.id.et_phone);

        etAddress = view.findViewById(R.id.et_address);
        etCity = view.findViewById(R.id.et_city);
        etZipCode = view.findViewById(R.id.et_zip_code);
        etCountry = view.findViewById(R.id.et_country);

        cardAgencyInfo = view.findViewById(R.id.card_agency_info);
        etAgencyName = view.findViewById(R.id.et_agency_name);
        etRegistrationNumber = view.findViewById(R.id.et_registration_number);
        etWebsite = view.findViewById(R.id.et_website);
        etDescription = view.findViewById(R.id.et_description);

        Button btnSaveChanges = view.findViewById(R.id.btn_save_changes);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        // Set click listeners
        btnChangePhoto.setOnClickListener(v -> openFileChooser());
        btnSaveChanges.setOnClickListener(v -> saveUserData());
        btnLogout.setOnClickListener(v -> logout());

        // Load user data
        loadUserData();

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(profileImage);
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadUserData() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set basic user information
                        userEmail.setText(mAuth.getCurrentUser().getEmail());

                        // Get user type
                        userTypeValue = documentSnapshot.getString("userType");
                        userType.setText(userTypeValue != null ? userTypeValue.substring(0, 1).toUpperCase() + userTypeValue.substring(1) : "");

                        // Get first and last name for both agency and client
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");

                        // Show/hide agency info based on user type
                        if ("agency".equals(userTypeValue)) {
                            cardAgencyInfo.setVisibility(View.VISIBLE);

                            // Set agency specific data
                            etAgencyName.setText(documentSnapshot.getString("agencyName"));
                            etRegistrationNumber.setText(documentSnapshot.getString("registrationNumber"));
                            etWebsite.setText(documentSnapshot.getString("website"));
                            etDescription.setText(documentSnapshot.getString("description"));

                            // Set name fields
                            etFirstName.setText(firstName);
                            etLastName.setText(lastName);

                            // Set name display - prefer agency name if available, otherwise use first and last name
                            String agencyName = documentSnapshot.getString("agencyName");
                            if (agencyName != null && !agencyName.isEmpty()) {
                                userName.setText(agencyName);
                            } else if (firstName != null && lastName != null) {
                                userName.setText(firstName + " " + lastName);
                            } else {
                                userName.setText("Agency User");
                            }
                        } else {
                            // Handle client data
                            // Set name fields
                            etFirstName.setText(firstName);
                            etLastName.setText(lastName);

                            // Set name display
                            if (firstName != null && lastName != null) {
                                userName.setText(firstName + " " + lastName);
                            } else {
                                userName.setText("Client User");
                            }
                        }

                        // Set common fields
                        etPhone.setText(documentSnapshot.getString("phone"));
                        etAddress.setText(documentSnapshot.getString("address"));
                        etCity.setText(documentSnapshot.getString("city"));
                        etZipCode.setText(documentSnapshot.getString("zipCode"));
                        etCountry.setText(documentSnapshot.getString("country"));

                        // Load profile image if available
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.placeholder_profile)
                                    .into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserData() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Map<String, Object> userData = new HashMap<>();

        // Save common fields
        userData.put("phone", Objects.requireNonNull(etPhone.getText()).toString().trim());
        userData.put("address", Objects.requireNonNull(etAddress.getText()).toString().trim());
        userData.put("city", Objects.requireNonNull(etCity.getText()).toString().trim());
        userData.put("zipCode", Objects.requireNonNull(etZipCode.getText()).toString().trim());
        userData.put("country", Objects.requireNonNull(etCountry.getText()).toString().trim());

        // Get first and last name for both agency and client
        String firstName = Objects.requireNonNull(etFirstName.getText()).toString().trim();
        String lastName = Objects.requireNonNull(etLastName.getText()).toString().trim();

        // Save user type specific fields
        if ("agency".equals(userTypeValue)) {
            // Agency-specific fields
            userData.put("agencyName", Objects.requireNonNull(etAgencyName.getText()).toString().trim());
            userData.put("registrationNumber", Objects.requireNonNull(etRegistrationNumber.getText()).toString().trim());
            userData.put("website", Objects.requireNonNull(etWebsite.getText()).toString().trim());
            userData.put("description", Objects.requireNonNull(etDescription.getText()).toString().trim());

            // Always save first and last name for agency users
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
        } else {
            // For client users
            // Only add first and last name if they are not empty
            if (!firstName.isEmpty()) {
                userData.put("firstName", firstName);
            }
            if (!lastName.isEmpty()) {
                userData.put("lastName", lastName);
            }
        }

        // If image was changed, upload it first
        if (imageUri != null) {
            uploadImage(userId, userData);
        } else {
            // Just update user data without changing image
            updateUserData(userId, userData);
        }
    }

    private void uploadImage(String userId, Map<String, Object> userData) {
        StorageReference fileReference = storageRef.child(userId + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        userData.put("profileImageUrl", uri.toString());
                        updateUserData(userId, userData);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserData(String userId, Map<String, Object> userData) {
        db.collection("users").document(userId)
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Reload user data to reflect changes
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }
}