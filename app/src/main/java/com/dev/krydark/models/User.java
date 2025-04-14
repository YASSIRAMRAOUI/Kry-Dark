package com.dev.krydark.models;

import java.util.Map;

public class User {
    private String id;
    private String userType; // "client" or "agency"
    private String email;
    private String firstName; // for clients
    private String lastName; // for clients
    private String agencyName; // for agencies
    private String registrationNumber; // for agencies
    private String phone;
    private String address;
    private String city;
    private String zipCode;
    private String country;
    private String website; // for agencies
    private String description; // for agencies
    private boolean verified; // for agencies
    private String profileImageUrl;
    private long createdAt;

    // No-argument constructor needed for Firestore
    public User() {}

    // Constructor for Client
    public User(String id, String email, String firstName, String lastName, String phone,
                String address, String city, String zipCode, String country, String profileImageUrl) {
        this.id = id;
        this.userType = "client";
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.zipCode = zipCode;
        this.country = country;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = System.currentTimeMillis();
    }

    // Constructor for Agency
    public User(String id, String email, String agencyName, String registrationNumber,
                String phone, String address, String city, String zipCode, String country,
                String website, String description, boolean verified, String profileImageUrl) {
        this.id = id;
        this.userType = "agency";
        this.email = email;
        this.agencyName = agencyName;
        this.registrationNumber = registrationNumber;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.zipCode = zipCode;
        this.country = country;
        this.website = website;
        this.description = description;
        this.verified = verified;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Helper methods
    public String getDisplayName() {
        if ("agency".equals(userType)) {
            return agencyName;
        } else {
            return firstName + " " + lastName;
        }
    }
}