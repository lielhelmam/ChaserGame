package com.example.chasergame.models;

public class Note {
    private long timestamp;
    private int lane;

    // Required for Firebase
    public Note() {}

    public Note(long timestamp, int lane) {
        this.timestamp = timestamp;
        this.lane = lane;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
}
