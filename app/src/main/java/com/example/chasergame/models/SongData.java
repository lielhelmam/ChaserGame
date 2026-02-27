package com.example.chasergame.models;

import java.util.List;

public class SongData {
    private String name;
    private String difficulty;
    private int resId; 
    private String resName; // Store resource name as string for easier Firebase handling
    private List<Note> notes;
    private int targetScore;

    // Required for Firebase
    public SongData() {}

    public SongData(String name, String difficulty, int resId, List<Note> notes, int targetScore) {
        this.name = name;
        this.difficulty = difficulty;
        this.resId = resId;
        this.notes = notes;
        this.targetScore = targetScore;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getResId() { return resId; }
    public void setResId(int resId) { this.resId = resId; }
    public String getResName() { return resName; }
    public void setResName(String resName) { this.resName = resName; }
    public List<Note> getNotes() { return notes; }
    public void setNotes(List<Note> notes) { this.notes = notes; }
    public int getTargetScore() { return targetScore; }
    public void setTargetScore(int targetScore) { this.targetScore = targetScore; }
}
