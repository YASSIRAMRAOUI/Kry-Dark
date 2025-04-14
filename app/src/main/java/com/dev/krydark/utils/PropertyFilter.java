package com.dev.krydark.utils;

import com.dev.krydark.models.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for filtering properties based on various criteria
 */
public class PropertyFilter {
    private double minPrice;
    private double maxPrice;
    private String location;
    private int bedroomCount;
    private int bathroomCount;
    private String searchQuery;

    // Constructor
    public PropertyFilter(double minPrice, double maxPrice, String location, int bedroomCount, int bathroomCount) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.location = location;
        this.bedroomCount = bedroomCount;
        this.bathroomCount = bathroomCount;
        this.searchQuery = "";
    }

    // Constructor with search query
    public PropertyFilter(double minPrice, double maxPrice, String location, int bedroomCount, int bathroomCount, String searchQuery) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.location = location;
        this.bedroomCount = bedroomCount;
        this.bathroomCount = bathroomCount;
        this.searchQuery = searchQuery != null ? searchQuery.toLowerCase() : "";
    }

    // Default constructor with no filters
    public PropertyFilter() {
        this.minPrice = 0;
        this.maxPrice = 1000000;
        this.location = "";
        this.bedroomCount = 0;
        this.bathroomCount = 0;
        this.searchQuery = "";
    }

    // Getters and Setters
    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getBedroomCount() { return bedroomCount; }
    public void setBedroomCount(int bedroomCount) { this.bedroomCount = bedroomCount; }

    public int getBathroomCount() { return bathroomCount; }
    public void setBathroomCount(int bathroomCount) { this.bathroomCount = bathroomCount; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery != null ? searchQuery.toLowerCase() : ""; }

    /**
     * Apply this filter to a list of properties
     *
     * @param properties List of properties to filter
     * @return Filtered list of properties
     */
    public List<Property> apply(List<Property> properties) {
        List<Property> filteredList = new ArrayList<>();

        for (Property property : properties) {
            if (matches(property)) {
                filteredList.add(property);
            }
        }

        return filteredList;
    }

    /**
     * Check if a property matches this filter
     *
     * @param property Property to check
     * @return true if the property matches, false otherwise
     */
    public boolean matches(Property property) {
        // Check price range
        if (property.getPrice() < minPrice || property.getPrice() > maxPrice) {
            return false;
        }

        // Check location if specified
        if (!location.isEmpty() && !property.getLocation().toLowerCase().contains(location.toLowerCase())) {
            return false;
        }

        // Check bedroom count if specified
        if (bedroomCount > 0 && property.getBedrooms() < bedroomCount) {
            return false;
        }

        // Check bathroom count if specified
        if (bathroomCount > 0 && property.getBathrooms() < bathroomCount) {
            return false;
        }

        // Check search query if specified
        if (!searchQuery.isEmpty()) {
            boolean matchesSearch = property.getTitle().toLowerCase().contains(searchQuery) ||
                    property.getDescription().toLowerCase().contains(searchQuery) ||
                    property.getLocation().toLowerCase().contains(searchQuery);

            return matchesSearch;
        }

        return true;
    }
}