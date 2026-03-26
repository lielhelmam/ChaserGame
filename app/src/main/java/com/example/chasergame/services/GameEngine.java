package com.example.chasergame.services;

public class GameEngine {
    private final int visibleSteps;
    private int p1Pos = 0;
    private int p2Pos = 0;
    private int p1Score = 0;
    private int p2Score = 0;
    private int currentPlayer = 1;

    public GameEngine(int visibleSteps) {
        this.visibleSteps = visibleSteps;
    }

    public void onCorrectAnswer() {
        if (currentPlayer == 1) {
            p1Pos++;
            p1Score++;
        } else {
            p2Pos++;
            p2Score++;
        }
    }

    public boolean isCaught() {
        return currentPlayer == 2 && p2Pos >= p1Pos;
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }

    public int getP1Pos() {
        return p1Pos;
    }

    public int getP2Pos() {
        return p2Pos;
    }

    public int getP1Score() {
        return p1Score;
    }

    public int getP2Score() {
        return p2Score;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int getVisibleSteps() {
        return visibleSteps;
    }
}
