package com.java.vbel.trashlocator.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;

public class PointSend {

    @SerializedName("userId")
    @Expose
    private long userId;

    @SerializedName("categoryId")
    @Expose
    private long categoryId;

    @SerializedName("lat")
    @Expose
    private double lat;

    @SerializedName("lng")
    @Expose
    private double lng;

    @SerializedName("date")
    @Expose
    private String date;



    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
