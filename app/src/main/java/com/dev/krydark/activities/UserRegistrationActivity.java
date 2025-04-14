package com.dev.krydark.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.HashMap;
import java.util.Map;

public class UserRegistrationActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    private EditText etAddress, etCity, etZipCode, etCountry;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etZipCode = findViewById(R.id.etZipCode);
        etCountry = findViewById(R.id.etCountry);

        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login
                Intent intent = new Intent(UserRegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        // Get input values
        final String firstName = etFirstName.getText().toString().trim();
        final String lastName = etLastName.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();
        final String address = etAddress.getText().toString().trim();
        final String city = etCity.getText().toString().trim();
        final String zipCode = etZipCode.getText().toString().trim();
        final String country = etCountry.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
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

        progressBar.setVisibility(View.VISIBLE);

        // Register the user in Firebase Auth
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
                                                // Save user details to Firestore
                                                saveUserDetailsToFirestore(user.getUid(), firstName, lastName, email, phone, address, city, zipCode, country);
                                            } else {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(UserRegistrationActivity.this,
                                                        "Failed to send verification email: " + task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            // If registration fails, display a message to the user
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UserRegistrationActivity.this, "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserDetailsToFirestore(String userId, String firstName, String lastName,
                                            String email, String phone, String address,
                                            String city, String zipCode, String country) {
        // Create a new user with all fields
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("phone", phone);
        user.put("address", address);
        user.put("city", city);
        user.put("zipCode", zipCode);
        user.put("country", country);
        user.put("userType", "client");
        user.put("createdAt", System.currentTimeMillis());

        // Add a new document with a generated ID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserRegistrationActivity.this, "Registration successful! Please check your email for verification.",
                                Toast.LENGTH_LONG).show();

                        // Navigate to login
                        Intent intent = new Intent(UserRegistrationActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(UserRegistrationActivity.this, "Error saving user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}