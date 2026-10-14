package com.example.dussehraphotoorganizer.model;

import java.io.Serializable;

/**
 * LocationConfigItem represents a registered physical location for EXIF GPS photo organization.
 * Contains exact address, target subfolder name (strictly formatted), GPS coordinates, and radius in meters.
 */
public class LocationConfigItem implements Serializable {

    private String id;
    private String name;
    private String teluguName;
    private String address;
    private String folderName;
    private double latitude;
    private double longitude;
    private float radiusMeters;
    private boolean enabled;

    public LocationConfigItem() {
        this.enabled = true;
    }

    public LocationConfigItem(String id, String name, String teluguName, String address, String folderName, double latitude, double longitude, float radiusMeters) {
        this.id = id;
        this.name = name;
        this.teluguName = teluguName;
        this.address = address;
        this.folderName = folderName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.enabled = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeluguName() {
        return teluguName;
    }

    public void setTeluguName(String teluguName) {
        this.teluguName = teluguName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(float radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
