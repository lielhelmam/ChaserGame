package com.example.chasergame.models;

public class Note {
    private long timestamp;
    private int lane;
    private long duration; // 0 for NORMAL, >0 for SLIDER
    private Type type = Type.NORMAL;

    // Required for Firebase
    public Note() {
    }

    public Note(long timestamp, int lane) {
        this(timestamp, lane, 0, Type.NORMAL);
    }

    public Note(long timestamp, int lane, long duration, Type type) {
        this.timestamp = timestamp;
        this.lane = lane;
        this.duration = duration;
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isSlider() {
        return type == Type.SLIDER;
    }

    public enum Type {
        NORMAL, SLIDER, SPINNER
    }
}
