package com.dev.krydark.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.dev.krydark.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        // Set up toolbar
        setupToolbar();

        // Initialize views and other setup
        initViews();
    }

    protected void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            // Hide the title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            // Set up logo click listener
            ImageView logoImage = toolbar.findViewById(R.id.toolbar_logo);
            if (logoImage != null) {
                logoImage.setOnClickListener(v -> {
                    // Check if we're already on the selection screen by comparing class names
                    if (!BaseActivity.this.getClass().getSimpleName().equals("SelectionActivity")) {
                        Intent intent = new Intent(BaseActivity.this, SelectionActivity.class);
                        // Clear the back stack so user can't go back
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
            }
        }
    }

    // Method to be implemented by child activities to return their layout resource ID
    protected abstract int getLayoutResourceId();

    // Method to be implemented by child activities to initialize their views
    protected abstract void initViews();
}