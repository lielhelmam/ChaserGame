package com.example.chasergame.services;

import com.example.chasergame.models.SongData;

public class RhythmGameManager {
    private final SongData songData;
    private int currentScore = 0;
    private int totalNotesPossible = 0;
    private double currentAccuracyWeight = 0;
    
    private int currentHp = 10;
    private final int MAX_HP = 10;
    private int currentCombo = 0;
    private int maxCombo = 0;
    private boolean isGameOver = false;

    public RhythmGameManager(SongData songData) {
        this.songData = songData;
    }

    public void onSliderStarted() {
        if (isGameOver) return;
        totalNotesPossible++;
    }

    public void onNoteHit(int points) {
        if (isGameOver) return;
        
        // Update Combo
        currentCombo++;
        if (currentCombo > maxCombo) maxCombo = currentCombo;

        // Scoring with Multiplier (Every 10 combo increases multiplier by 0.1x)
        float multiplier = 1.0f + (currentCombo / 10f) * 0.1f;
        currentScore += (int) (points * multiplier);

        // Accuracy Calculation
        totalNotesPossible++;
        if (points == 300) currentAccuracyWeight += 1.0;
        else if (points == 150) currentAccuracyWeight += 0.75; // Slider partial
        else if (points == 100) currentAccuracyWeight += 0.5;
        
        // HP gain logic (Perfects heal more)
        int hpGain = (points == 300) ? 1 : 0;
        currentHp = Math.min(MAX_HP, currentHp + hpGain);
    }

    public void onNoteMissed(boolean wasAlreadyStarted) {
        if (isGameOver) return;
        
        // Combo Break!
        currentCombo = 0;
        
        if (!wasAlreadyStarted) {
            totalNotesPossible++;
        }

        // Steeper HP loss on miss
        currentHp -= (songData != null ? songData.getHpDrain() : 2);
        
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

    public int getCurrentCombo() {
        return currentCombo;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public double getAccuracy() {
        if (totalNotesPossible == 0) return 100.0;
        return (currentAccuracyWeight / totalNotesPossible) * 100.0;
    }

    public String getRank() {
        double acc = getAccuracy();
        if (acc >= 95) return "S";
        if (acc >= 85) return "A";
        if (acc >= 75) return "B";
        if (acc >= 65) return "C";
        return "D";
    }

    public int calculateEarnedPoints() {
        if (isGameOver) return (currentScore / 15); // Penalty for failing
        
        int basePoints = currentScore / 10;
        // Bonus for Rank
        String rank = getRank();
        if (rank.equals("S")) basePoints += 1000;
        else if (rank.equals("A")) basePoints += 500;
        
        // Bonus for Max Combo
        basePoints += (maxCombo * 10);
        
        return basePoints;
    }
}
