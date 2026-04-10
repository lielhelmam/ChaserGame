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
        // Sliders are counted as a single perfect hit for accuracy if started
        totalNotesPossible++;
        currentAccuracyWeight += 1.0;
    }

    public void onNoteHit(int points) {
        if (isGameOver) return;
        
        currentCombo++;
        if (currentCombo > maxCombo) maxCombo = currentCombo;

        float multiplier = 1.0f + (currentCombo / 10f) * 0.1f;
        currentScore += (int) (points * multiplier);

        // Accuracy Calculation (Only for normal notes AND slider head)
        // We skip 150 because it's used for slider continuous ticks/perfect finish
        if (points != 150) { 
            totalNotesPossible++;
            if (points == 300) currentAccuracyWeight += 1.0;
            else if (points == 100) currentAccuracyWeight += 0.5;
        }
        
        int hpGain = (points == 300) ? 1 : 0;
        currentHp = Math.min(MAX_HP, currentHp + hpGain);
    }

    public void onNoteMissed(boolean wasAlreadyStarted) {
        if (isGameOver) return;
        currentCombo = 0;
        
        // If it was a normal note or a slider that wasn't even touched
        if (!wasAlreadyStarted) {
            totalNotesPossible++;
        } else {
            // If they started a slider but let go, the weight was already added in onSliderStarted,
            // so we don't need to add to totalNotesPossible, but we failed the weight.
            // To simplify, we'll just let the HP drain handle the penalty.
        }

        currentHp -= (songData != null ? songData.getHpDrain() : 2);
        if (currentHp <= 0) {
            currentHp = 0;
            isGameOver = true;
        }
    }

    public int getCurrentScore() { return currentScore; }
    public int getCurrentHp() { return currentHp; }
    public int getCurrentCombo() { return currentCombo; }
    public int getMaxCombo() { return maxCombo; }
    public boolean isGameOver() { return isGameOver; }

    public double getAccuracy() {
        if (totalNotesPossible == 0) return 100.0;
        return Math.min(100.0, (currentAccuracyWeight / totalNotesPossible) * 100.0);
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
        if (isGameOver) return (currentScore / 15);
        int basePoints = currentScore / 10;
        String rank = getRank();
        if (rank.equals("S")) basePoints += 1000;
        else if (rank.equals("A")) basePoints += 500;
        basePoints += (maxCombo * 10);
        return basePoints;
    }
}
