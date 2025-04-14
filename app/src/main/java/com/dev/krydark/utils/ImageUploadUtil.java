package com.dev.krydark.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.dev.krydark.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Utility class for image upload operations
 */
public class ImageUploadUtil {
    private static FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Upload a single image to Firebase Storage
     *
     * @param context Application context
     * @param imageUri Uri of the image to upload
     * @param path Storage path (e.g., "profile_images/")
     * @param filename Filename to use in storage
     * @param listener Callback for upload events
     */
    public static void uploadImage(Context context, Uri imageUri, String path, String filename, OnImageUploadListener listener) {
        if (imageUri == null) {
            listener.onError("No image selected");
            return;
        }

        StorageReference storageRef = storage.getReference().child(path).child(filename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        listener.onSuccess(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    listener.onError(e.getMessage());
                    Toast.makeText(context, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    listener.onProgress((int) progress);
                });
    }

    /**
     * Upload multiple images to Firebase Storage
     *
     * @param context Application context
     * @param imageUris List of image URIs to upload
     * @param path Storage path (e.g., "property_images/")
     * @param baseName Base name for files (will be appended with index)
     * @param listener Callback for upload events
     */
    public static void uploadMultipleImages(Context context, java.util.List<Uri> imageUris, String path, String baseName, OnMultipleImagesUploadListener listener) {
        if (imageUris == null || imageUris.isEmpty()) {
            listener.onError("No images selected");
            return;
        }

        java.util.List<String> uploadedImageUrls = new java.util.ArrayList<>();
        final int[] uploadedCount = {0};
        final int totalCount = imageUris.size();

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            String filename = baseName + "_" + i + ".jpg";

            StorageReference storageRef = storage.getReference().child(path).child(filename);

            int finalI = i;
            storageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedImageUrls.add(uri.toString());
                            uploadedCount[0]++;

                            listener.onProgress(uploadedCount[0], totalCount);

                            if (uploadedCount[0] == totalCount) {
                                // All images uploaded
                                listener.onSuccess(uploadedImageUrls);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        listener.onError("Failed to upload image " + (finalI + 1) + ": " + e.getMessage());
                        Toast.makeText(context, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Load an image into an ImageView using Glide
     *
     * @param context Application context
     * @param imageUrl URL of the image to load
     * @param imageView ImageView to load the image into
     * @param placeholderResId Resource ID for the placeholder image
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView, int placeholderResId) {
        Glide.with(context)
                .load(imageUrl)
                .placeholder(placeholderResId)
                .into(imageView);
    }

    // Default method with default placeholder
    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        loadImage(context, imageUrl, imageView, R.drawable.placeholder_property);
    }

    // Interface for image upload callback
    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
        void onProgress(int progress);
    }

    // Interface for multiple images upload callback
    public interface OnMultipleImagesUploadListener {
        void onSuccess(java.util.List<String> imageUrls);
        void onError(String errorMessage);
        void onProgress(int uploadedCount, int totalCount);
    }
}