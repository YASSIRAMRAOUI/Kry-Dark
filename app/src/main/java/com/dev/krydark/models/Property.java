package com.dev.krydark.models;

import java.util.List;

public class Property {
    private String id;
    private String ownerId;
    private String title;
    private String description;
    private double price;
    private String location;
    private int size;
    private int bedrooms;
    private int bathrooms;
    private List<String> amenities;
    private List<String> imageUrls;
    private long createdAt;

    // No-argument constructor needed for Firestore
    public Property() {}

    // Constructor
    public Property(String ownerId, String title, String description, double price,
                    String location, int size, int bedrooms, int bathrooms,
                    List<String> amenities, List<String> imageUrls) {
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.size = size;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.amenities = amenities;
        this.imageUrls = imageUrls;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(int bathrooms) {
        this.bathrooms = bathrooms;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}