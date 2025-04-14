package com.dev.krydark.activities;

import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.dev.krydark.R;
import com.dev.krydark.fragments.AddPropertyFragment;
import com.dev.krydark.fragments.ChatFragment;
import com.dev.krydark.fragments.FavoritesFragment;
import com.dev.krydark.fragments.HomeFragment;
import com.dev.krydark.fragments.ProfileFragment;
import com.dev.krydark.fragments.ReservationsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isAgencyUser = false;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initViews() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupNavigationListener();

        // Check user role and set up the UI accordingly
        checkUserRoleAndSetupUI(currentUser.getUid());

        // Set Home as default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    private void checkUserRoleAndSetupUI(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        if (userType != null && userType.equals("agency")) {
                            // This is an agency user
                            isAgencyUser = true;

                            // Hide favorites for agency users
                            if (bottomNavigationView.getMenu().findItem(R.id.navigation_favorites) != null) {
                                bottomNavigationView.getMenu().findItem(R.id.navigation_favorites).setVisible(false);
                            }
                        } else {
                            // This is a regular client user
                            isAgencyUser = false;

                            // Show favorites for clients
                            if (bottomNavigationView.getMenu().findItem(R.id.navigation_favorites) != null) {
                                bottomNavigationView.getMenu().findItem(R.id.navigation_favorites).setVisible(true);
                            }
                        }
                    } else {
                        // Default to client view if user document doesn't exist
                        setupClientUI();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure by defaulting to client view
                    setupClientUI();
                });
    }

    private void setupClientUI() {
        isAgencyUser = false;

        if (bottomNavigationView.getMenu().findItem(R.id.navigation_favorites) != null) {
            bottomNavigationView.getMenu().findItem(R.id.navigation_favorites).setVisible(true);
        }
    }

    private void setupNavigationListener() {
        // Set the navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (itemId == R.id.navigation_reservations) {
                selectedFragment = new ReservationsFragment();
            } else if (itemId == R.id.navigation_chat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    private void navigateToLogin() {
        // Navigate to login
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}