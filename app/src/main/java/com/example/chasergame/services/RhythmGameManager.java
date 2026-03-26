package com.example.chasergame.services;

import com.example.chasergame.models.SongData;

public class RhythmGameManager {
    private final SongData songData;
    private int currentScore = 0;
    private int totalNotesPassed = 0;
    private double notesHitWeight = 0;

    public RhythmGameManager(SongData songData) {
        this.songData = songData;
    }

    public void onNoteHit(int points) {
        totalNotesPassed++;
        notesHitWeight += (points == 300) ? 1.0 : 0.5;
        currentScore += points;
    }

    public void onNoteMissed() {
        totalNotesPassed++;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public double getAccuracy() {
        if (totalNotesPassed == 0) return 0;
        return (notesHitWeight / totalNotesPassed) * 100.0;
    }

    public boolean isLevelPassed() {
        return songData != null && currentScore >= songData.getTargetScore();
    }

    public int calculateEarnedPoints() {
        boolean passed = isLevelPassed();
        int points;
        if (passed) {
            points = currentScore / 10;
            if (getAccuracy() >= 95.0) points += 500;
        } else {
            points = (currentScore / 10) / 3;
        }
        return points;
    }

    public int getTotalNotesPassed() {
        return totalNotesPassed;
    }
}
