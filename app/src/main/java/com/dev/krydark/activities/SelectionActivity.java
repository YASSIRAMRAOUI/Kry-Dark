package com.dev.krydark.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.dev.krydark.R;

public class SelectionActivity extends AppCompatActivity {

    private CardView userCard, agencyCard;
    private TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        // Initialize views
        userCard = findViewById(R.id.userCard);
        agencyCard = findViewById(R.id.agencyCard);
        loginLink = findViewById(R.id.loginLink);

        // Set click listeners
        userCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to user registration
                Intent intent = new Intent(SelectionActivity.this, UserRegistrationActivity.class);
                startActivity(intent);
            }
        });

        agencyCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to agency registration
                Intent intent = new Intent(SelectionActivity.this, AgencyRegistrationActivity.class);
                startActivity(intent);
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login page
                Intent intent = new Intent(SelectionActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}