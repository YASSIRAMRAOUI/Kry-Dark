package com.dev.krydark.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.krydark.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AgencyRegistrationActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etAgencyName, etRegistrationNumber, etEmail, etPhone, etPassword, etConfirmPassword;
    private EditText etAddress, etCity, etZipCode, etCountry, etDescription, etWebsite;
    private Button btnRegister, btnUploadLicense;
    private TextView tvLoginLink;
    private ImageView ivLicensePreview;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri licenseImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agency_registration);

        // Initialize Firebase Auth, Firestore and Storage
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("licenses");

        // Initialize views
        etAgencyName = findViewById(R.id.etAgencyName);
        etRegistrationNumber = findViewById(R.id.etRegistrationNumber);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etZipCode = findViewById(R.id.etZipCode);
        etCountry = findViewById(R.id.etCountry);
        etDescription = findViewById(R.id.etDescription);
        etWebsite = findViewById(R.id.etWebsite);

        btnRegister = findViewById(R.id.btnRegister);
        btnUploadLicense = findViewById(R.id.btnUploadLicense);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        ivLicensePreview = findViewById(R.id.ivLicensePreview);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerAgency();
            }
        });

        btnUploadLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login
                Intent intent = new Intent(AgencyRegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            licenseImageUri = data.getData();
            ivLicensePreview.setImageURI(licenseImageUri);
            ivLicensePreview.setVisibility(View.VISIBLE);
        }
    }

    private void registerAgency() {
        // Get input values
        final String agencyName = etAgencyName.getText().toString().trim();
        final String registrationNumber = etRegistrationNumber.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();
        final String address = etAddress.getText().toString().trim();
        final String city = etCity.getText().toString().trim();
        final String zipCode = etZipCode.getText().toString().trim();
        final String country = etCountry.getText().toString().trim();
        final String description = etDescription.getText().toString().trim();
        final String website = etWebsite.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(agencyName)) {
            etAgencyName.setError("Agency name is required");
            etAgencyName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(registrationNumber)) {
            etRegistrationNumber.setError("Registration number is required");
            etRegistrationNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password should be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (licenseImageUri == null) {
            Toast.makeText(this, "Please upload a license document", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Register the agency in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, now send verification email
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Send email verification
                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                // Upload license image and save agency details
                                                uploadLicenseAndSaveAgency(user.getUid(), agencyName, registrationNumber,
                                                        email, phone, address, city, zipCode,
                                                        country, description, website);
                                            } else {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(AgencyRegistrationActivity.this,
                                                        "Failed to send verification email: " + task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            // If registration fails, display a message to the user
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AgencyRegistrationActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void uploadLicenseAndSaveAgency(final String userId, final String agencyName,
                                            final String registrationNumber, final String email,
                                            final String phone, final String address,
                                            final String city, final String zipCode,
                                            final String country, final String description,
                                            final String website) {

        // Generate unique filename for the license image
        final String licenseName = UUID.randomUUID().toString();

        // Upload to Firebase Storage
        StorageReference fileReference = storageRef.child(licenseName);
        fileReference.putFile(licenseImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get download URL
                        fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                // Now save agency details with license URL
                                saveAgencyDetailsToFirestore(userId, agencyName, registrationNumber,
                                        email, phone, address, city, zipCode,
                                        country, description, website,
                                        downloadUri.toString());
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AgencyRegistrationActivity.this, "Failed to upload license: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAgencyDetailsToFirestore(String userId, String agencyName, String registrationNumber,
                                              String email, String phone, String address,
                                              String city, String zipCode, String country,
                                              String description, String website, String licenseUrl) {

        // Create a new agency document
        Map<String, Object> agency = new HashMap<>();
        agency.put("agencyName", agencyName);
        agency.put("registrationNumber", registrationNumber);
        agency.put("email", email);
        agency.put("phone", phone);
        agency.put("address", address);
        agency.put("city", city);
        agency.put("zipCode", zipCode);
        agency.put("country", country);
        agency.put("description", description);
        agency.put("website", website);
        agency.put("licenseUrl", licenseUrl);
        agency.put("userType", "agency");
        agency.put("verified", false); // Admin needs to verify agency
        agency.put("createdAt", System.currentTimeMillis());

        // Add agency to Firestore
        db.collection("users").document(userId)
                .set(agency)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AgencyRegistrationActivity.this,
                                "Registration successful! Please check your email for verification and wait for admin approval.",
                                Toast.LENGTH_LONG).show();

                        // Navigate to login
                        Intent intent = new Intent(AgencyRegistrationActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AgencyRegistrationActivity.this, "Error saving agency data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}