package com.example.dussehraphotoorganizer.model;

import java.io.Serializable;

public class FestivalDateItem implements Serializable {
    private String date; // Format: dd-MM-yyyy (e.g. 11-10-2026)
    private String dayTithi; // e.g. ఆదివారం శుద్ధ పాడ్యమి
    private String teluguName; // e.g. శ్రీ బాలాత్రిపురసుందరి దేవి
    private String englishName; // e.g. Sri Bala Tripura Sundari Devi
    private String folderName; // STRICTLY WITHOUT DATE: e.g. Sri_Bala_Tripura_Sundari_Devi
    private String specialProgram;
    private int photoCount;

    public FestivalDateItem() {}

    public FestivalDateItem(String date, String dayTithi, String teluguName, String englishName, String folderName, String specialProgram) {
        this.date = date;
        this.dayTithi = dayTithi;
        this.teluguName = teluguName;
        this.englishName = englishName;
        this.folderName = folderName;
        this.specialProgram = specialProgram;
        this.photoCount = 0;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayTithi() {
        return dayTithi;
    }

    public void setDayTithi(String dayTithi) {
        this.dayTithi = dayTithi;
    }

    public String getTeluguName() {
        return teluguName;
    }

    public void setTeluguName(String teluguName) {
        this.teluguName = teluguName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getSpecialProgram() {
        return specialProgram;
    }

    public void setSpecialProgram(String specialProgram) {
        this.specialProgram = specialProgram;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    public void setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
    }
}
