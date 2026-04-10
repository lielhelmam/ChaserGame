package com.example.chasergame.models;

import java.util.List;

public class SongData {
    private String name;
    private String difficulty;
    private int resId;
    private String resName;
    private List<Note> notes;
    private int bpm = 120;
    private int hpDrain = 1; 
    private int hpGain = 1;  
    
    // Leaderboard Data
    private int topScore = 0;
    private String topPlayerName = "None";
    private String topRank = "-";
    private double topAccuracy = 0.0;

    public SongData() {}

    public SongData(String name, String difficulty, int resId, List<Note> notes, int hpDrain, int hpGain) {
        this.name = name;
        this.difficulty = difficulty;
        this.resId = resId;
        this.notes = notes;
        this.hpDrain = hpDrain;
        this.hpGain = hpGain;
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
    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }
    public int getHpDrain() { return hpDrain; }
    public void setHpDrain(int hpDrain) { this.hpDrain = hpDrain; }
    public int getHpGain() { return hpGain; }
    public void setHpGain(int hpGain) { this.hpGain = hpGain; }

    public int getTopScore() { return topScore; }
    public void setTopScore(int topScore) { this.topScore = topScore; }
    public String getTopPlayerName() { return topPlayerName; }
    public void setTopPlayerName(String topPlayerName) { this.topPlayerName = topPlayerName; }
    public String getTopRank() { return topRank; }
    public void setTopRank(String topRank) { this.topRank = topRank; }
    public double getTopAccuracy() { return topAccuracy; }
    public void setTopAccuracy(double topAccuracy) { this.topAccuracy = topAccuracy; }
}
