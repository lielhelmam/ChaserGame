package com.example.chasergame.services;

import com.example.chasergame.models.SongData;

public class RhythmGameManager {
    private final SongData songData;
    private int currentScore = 0;
    private int totalNotesPassed = 0;
    private double notesHitWeight = 0;
    
    private int currentHp = 10;
    private final int MAX_HP = 10;
    private boolean isGameOver = false;

    public RhythmGameManager(SongData songData) {
        this.songData = songData;
    }

    public void onSliderStarted() {
        if (isGameOver) return;
        totalNotesPassed++; // Count slider as 1 note at start
    }

    public void onNoteHit(int points) {
        if (isGameOver) return;
        
        if (points == 300 || points == 100) {
            totalNotesPassed++;
            notesHitWeight += (points == 300) ? 1.0 : 0.5;
        } else if (points == 150) { // Slider completion
            // totalNotesPassed was already incremented in onSliderStarted
            notesHitWeight += 1.0; 
        }
        
        currentScore += points;
        
        // HP gain logic
        if (points == 300 || points == 150) {
            currentHp = Math.min(MAX_HP, currentHp + (songData != null ? songData.getHpGain() : 1));
        }
    }

    public void onNoteMissed(boolean wasAlreadyStarted) {
        if (isGameOver) return;
        
        if (!wasAlreadyStarted) {
            totalNotesPassed++;
        }

        currentHp -= (songData != null ? songData.getHpDrain() : 1);
        
        if (currentHp <= 0) {
            currentHp = 0;
            isGameOver = true;
        }
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public double getAccuracy() {
        if (totalNotesPassed == 0) return 0;
        return (notesHitWeight / totalNotesPassed) * 100.0;
    }

    public int calculateEarnedPoints() {
        // If lost, only 1/3 of the potential points
        if (isGameOver) {
            return (currentScore / 10) / 3;
        }
        
        // If survived till the end
        int points = currentScore / 10;
        if (getAccuracy() >= 95.0) {
            points += 500;
        }
        return points;
    }

    public int getTotalNotesPassed() {
        return totalNotesPassed;
    }
}
