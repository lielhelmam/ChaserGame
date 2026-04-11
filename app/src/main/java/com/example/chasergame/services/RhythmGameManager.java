package com.example.chasergame.services;

import com.example.chasergame.models.SongData;

public class RhythmGameManager {
    private final SongData songData;
    private final int MAX_HP = 10;
    private int currentScore = 0;
    private int totalNotesPossible = 0;
    private double currentAccuracyWeight = 0;
    private int currentHp = 10;
    private int currentCombo = 0;
    private int maxCombo = 0;
    private boolean isGameOver = false;
    private double scoreMultiplier = 1.0;

    public RhythmGameManager(SongData songData) {
        this.songData = songData;
    }

    public void setScoreMultiplier(double multiplier) {
        this.scoreMultiplier = multiplier;
    }

    public void onSliderStarted() {
        if (isGameOver) return;
        // Logic for starting a slider (e.g., sound or visual)
        // We no longer increment totalNotesPossible here to avoid early Acc drops.
    }

    public void onNoteHit(int points) {
        if (isGameOver) return;

        currentCombo++;
        if (currentCombo > maxCombo) maxCombo = currentCombo;

        float multiplier = (1.0f + (currentCombo / 10f) * 0.1f) * (float) scoreMultiplier;
        currentScore += (int) (points * multiplier);

        // Accuracy Calculation
        if (points == 300) {
            totalNotesPossible++;
            currentAccuracyWeight += 1.0;
        } else if (points == 100) {
            totalNotesPossible++;
            currentAccuracyWeight += 0.5;
        } else if (points == 150) {
            // Slider completion - counted as a full note for accuracy
            totalNotesPossible++;
            currentAccuracyWeight += 1.0;
        } else if (points == 20) {
            // Slider tick - strictly for score, does not affect accuracy %
        }

        int hpGain = (points == 300 || points == 150) ? 1 : 0;
        currentHp = Math.min(MAX_HP, currentHp + hpGain);
    }

    public void onNoteMissed(boolean wasAlreadyStarted) {
        if (isGameOver) return;
        currentCombo = 0;

        // Any miss (normal or slider release) increments the denominator
        totalNotesPossible++;
        // currentAccuracyWeight does not increase, so Acc drops.

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
        return Math.min(100.0, (currentAccuracyWeight / totalNotesPossible) * 100.0);
    }

    public String getRank() {
        double acc = getAccuracy();
        if (acc >= 100.0) return "SS";
        if (acc >= 95.0) return "S";
        if (acc >= 90.0) return "A";
        if (acc >= 80.0) return "B";
        if (acc >= 70.0) return "C";
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
