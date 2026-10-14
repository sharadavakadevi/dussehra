package com.example.dussehraphotoorganizer.model;

import android.net.Uri;
import java.io.Serializable;

public class PhotoItem implements Serializable {
    private long id;
    private String fileName;
    private String filePath;
    private String uriString;
    private long captureTimestamp;
    private String captureDateFormatted;
    private String destinationFolderName;
    private String rootDirectory; // "Dussehra_2026" or "Location_Organized"
    private boolean isCameraPhoto;
    private boolean isSelected;
    private String statusMessage;
    private Double latitude;
    private Double longitude;
    private String locationLabel;

    public PhotoItem() {
        this.isSelected = true;
    }

    public PhotoItem(long id, String fileName, String filePath, Uri uri, long captureTimestamp, String captureDateFormatted, String destinationFolderName, boolean isCameraPhoto) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uriString = (uri != null) ? uri.toString() : "";
        this.captureTimestamp = captureTimestamp;
        this.captureDateFormatted = captureDateFormatted;
        this.destinationFolderName = destinationFolderName;
        this.rootDirectory = "Dussehra_2026";
        this.isCameraPhoto = isCameraPhoto;
        this.isSelected = true;
        this.statusMessage = "Eligible Camera Photo";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUriString() {
        return uriString;
    }

    public void setUriString(String uriString) {
        this.uriString = uriString;
    }

    public Uri getUri() {
        return (uriString != null && !uriString.isEmpty()) ? Uri.parse(uriString) : null;
    }

    public long getCaptureTimestamp() {
        return captureTimestamp;
    }

    public void setCaptureTimestamp(long captureTimestamp) {
        this.captureTimestamp = captureTimestamp;
    }

    public String getCaptureDateFormatted() {
        return captureDateFormatted;
    }

    public void setCaptureDateFormatted(String captureDateFormatted) {
        this.captureDateFormatted = captureDateFormatted;
    }

    public String getDestinationFolderName() {
        return destinationFolderName;
    }

    public void setDestinationFolderName(String destinationFolderName) {
        this.destinationFolderName = destinationFolderName;
    }

    public String getRootDirectory() {
        return (rootDirectory != null && !rootDirectory.isEmpty()) ? rootDirectory : "Dussehra_2026";
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public boolean isCameraPhoto() {
        return isCameraPhoto;
    }

    public void setCameraPhoto(boolean cameraPhoto) {
        isCameraPhoto = cameraPhoto;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public void setLocationLabel(String locationLabel) {
        this.locationLabel = locationLabel;
    }
}
