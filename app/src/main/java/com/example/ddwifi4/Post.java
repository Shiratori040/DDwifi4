package com.example.ddwifi4;

public class Post {
    private int id;
    private String content;
    private String timestamp;
    private long expiryTimestamp;
    private double latitude;
    private double longitude;

    public Post(int id, String content, String timestamp, long expiryTimestamp, double latitude, double longitude) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public void setExpiryTimestamp(long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
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
}
