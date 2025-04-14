package com.dev.krydark.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dev.krydark.R;
import com.dev.krydark.adapters.ReservationAdapter;
import com.dev.krydark.models.Reservation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReservationsFragment extends Fragment implements ReservationAdapter.ReservationClickListener {
    private RecyclerView reservationsRecyclerView;
    private TextView emptyView;
    private ReservationAdapter reservationAdapter;
    private List<Reservation> reservationList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        reservationList = new ArrayList<>();

        // Initialize views
        reservationsRecyclerView = view.findViewById(R.id.reservations_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        // Setup recycler view
        reservationAdapter = new ReservationAdapter(reservationList, getContext(), this);
        reservationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reservationsRecyclerView.setAdapter(reservationAdapter);

        // Get user type first, then load reservations accordingly
        getUserTypeAndLoadReservations();

        return view;
    }

    private void getUserTypeAndLoadReservations() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userType = documentSnapshot.getString("userType");
                        loadReservations(userType);
                    } else {
                        // Default to regular user if user document doesn't exist
                        loadReservations("regular");
                    }
                })
                .addOnFailureListener(e -> {
                    // Default to regular user on error
                    loadReservations("regular");
                    Toast.makeText(getContext(), "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReservations(String userType) {
        String userId = mAuth.getCurrentUser().getUid();

        if ("agency".equals(userType)) {
            // For agencies, fetch all properties owned by this agency
            db.collection("properties")
                    .whereEqualTo("ownerId", userId)
                    .get()
                    .addOnSuccessListener(propertyDocuments -> {
                        List<String> propertyIds = new ArrayList<>();

                        // No properties found
                        if (propertyDocuments.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            reservationsRecyclerView.setVisibility(View.GONE);
                            return;
                        }

                        // Collect all property IDs
                        for (QueryDocumentSnapshot doc : propertyDocuments) {
                            propertyIds.add(doc.getId());
                        }

                        // Now fetch all reservations for these properties
                        if (!propertyIds.isEmpty()) {
                            db.collection("reservations")
                                    .whereIn("propertyId", propertyIds)
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            QuerySnapshot querySnapshot = task.getResult();
                                            processReservationResults(querySnapshot);
                                        } else {
                                            emptyView.setVisibility(View.VISIBLE);
                                            reservationsRecyclerView.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Error loading reservations", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    });
        } else {
            // For regular users, only show their reservations
            db.collection("reservations")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            processReservationResults(querySnapshot);
                        } else {
                            emptyView.setVisibility(View.VISIBLE);
                            reservationsRecyclerView.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Error loading reservations", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void processReservationResults(QuerySnapshot querySnapshot) {
        if (querySnapshot.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            reservationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            reservationsRecyclerView.setVisibility(View.VISIBLE);

            // Clear previous list
            reservationList.clear();

            for (QueryDocumentSnapshot document : querySnapshot) {
                String reservationId = document.getId();
                String propertyId = document.getString("propertyId");
                String userId = document.getString("userId");
                String status = document.getString("status");
                Long createdAtLong = document.getLong("createdAt");
                long createdAt = createdAtLong != null ? createdAtLong : 0;

                // Get property details
                db.collection("properties")
                        .document(propertyId)
                        .get()
                        .addOnSuccessListener(propertyDoc -> {
                            if (propertyDoc.exists()) {
                                String ownerId = propertyDoc.getString("ownerId");
                                String title = propertyDoc.getString("title");
                                String location = propertyDoc.getString("location");
                                List<String> imageUrls = (List<String>) propertyDoc.get("imageUrls");
                                String imageUrl = imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;

                                // If this is an agency, we also need user (client) details
                                if ("agency".equals(userType)) {
                                    // Get client details
                                    db.collection("users")
                                            .document(userId)
                                            .get()
                                            .addOnSuccessListener(clientDoc -> {
                                                if (clientDoc.exists()) {
                                                    String firstName = clientDoc.getString("firstName");
                                                    String lastName = clientDoc.getString("lastName");
                                                    String clientName = (firstName != null ? firstName : "") + " " +
                                                            (lastName != null ? lastName : "");
                                                    String phone = clientDoc.getString("phone");

                                                    Reservation reservation = new Reservation(
                                                            reservationId,
                                                            userId,
                                                            propertyId,
                                                            status,
                                                            createdAt,
                                                            title,
                                                            location,
                                                            imageUrl,
                                                            userId, // For agencies, we want to contact the client
                                                            clientName,
                                                            phone
                                                    );

                                                    reservationList.add(reservation);
                                                    reservationAdapter.notifyDataSetChanged();
                                                }
                                            });
                                } else {
                                    // Get owner details - for regular users viewing their reservations
                                    db.collection("users")
                                            .document(ownerId)
                                            .get()
                                            .addOnSuccessListener(ownerDoc -> {
                                                if (ownerDoc.exists()) {
                                                    String ownerName;
                                                    String phone = ownerDoc.getString("phone");

                                                    if ("agency".equals(ownerDoc.getString("userType"))) {
                                                        ownerName = ownerDoc.getString("agencyName");
                                                    } else {
                                                        String firstName = ownerDoc.getString("firstName");
                                                        String lastName = ownerDoc.getString("lastName");
                                                        ownerName = (firstName != null ? firstName : "") + " " +
                                                                (lastName != null ? lastName : "");
                                                    }

                                                    Reservation reservation = new Reservation(
                                                            reservationId,
                                                            userId,
                                                            propertyId,
                                                            status,
                                                            createdAt,
                                                            title,
                                                            location,
                                                            imageUrl,
                                                            ownerId,
                                                            ownerName,
                                                            phone
                                                    );

                                                    reservationList.add(reservation);
                                                    reservationAdapter.notifyDataSetChanged();
                                                }
                                            });
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void onCallClick(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    // Fix for ReservationsFragment.java - onMessageClick method
    @Override
    public void onMessageClick(String recipientId, String propertyId) {
        // Navigate to chat detail fragment
        Bundle args = new Bundle();

        // Change from recipientId to otherUserId to match what ChatDetailFragment expects
        args.putString("otherUserId", recipientId);
        args.putString("propertyId", propertyId);

        // Add additional necessary arguments
        // We'll fetch user information to provide to the chat fragment
        db.collection("users")
                .document(recipientId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get user info
                        String userName;
                        String userImage = documentSnapshot.getString("profileImageUrl");

                        if ("agency".equals(documentSnapshot.getString("userType"))) {
                            userName = documentSnapshot.getString("agencyName");
                        } else {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            userName = (firstName != null ? firstName : "") + " " +
                                    (lastName != null ? lastName : "");
                        }

                        // Also get property title if propertyId is available
                        if (propertyId != null && !propertyId.isEmpty()) {
                            db.collection("properties")
                                    .document(propertyId)
                                    .get()
                                    .addOnSuccessListener(propertyDoc -> {
                                        String title = "";
                                        if (propertyDoc.exists()) {
                                            title = propertyDoc.getString("title");
                                        }

                                        // Now we have all needed information, navigate to chat
                                        args.putString("otherUserName", userName);
                                        args.putString("otherUserImage", userImage);
                                        args.putString("propertyTitle", title);

                                        ChatDetailFragment chatFragment = new ChatDetailFragment();
                                        chatFragment.setArguments(args);

                                        getParentFragmentManager().beginTransaction()
                                                .replace(R.id.fragment_container, chatFragment)
                                                .addToBackStack(null)
                                                .commit();
                                    })
                                    .addOnFailureListener(e -> {
                                        // If property fetch fails, still navigate but without property info
                                        args.putString("otherUserName", userName);
                                        args.putString("otherUserImage", userImage);

                                        ChatDetailFragment chatFragment = new ChatDetailFragment();
                                        chatFragment.setArguments(args);

                                        getParentFragmentManager().beginTransaction()
                                                .replace(R.id.fragment_container, chatFragment)
                                                .addToBackStack(null)
                                                .commit();
                                    });
                        } else {
                            // No property ID, navigate with just user info
                            args.putString("otherUserName", userName);
                            args.putString("otherUserImage", userImage);

                            ChatDetailFragment chatFragment = new ChatDetailFragment();
                            chatFragment.setArguments(args);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, chatFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    } else {
                        // If user document doesn't exist, show error
                        Toast.makeText(getContext(), "Error: User information not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeleteClick(String reservationId, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Reservation")
                .setMessage("Are you sure you want to delete this reservation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete from Firestore
                    db.collection("reservations")
                            .document(reservationId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from adapter
                                reservationAdapter.removeItem(position);

                                // Show empty view if no items left
                                if (reservationList.isEmpty()) {
                                    emptyView.setVisibility(View.VISIBLE);
                                    reservationsRecyclerView.setVisibility(View.GONE);
                                }

                                Toast.makeText(getContext(), "Reservation deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete reservation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }
}