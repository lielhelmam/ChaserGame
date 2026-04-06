package com.example.chasergame.services;

public class GameEngine {
    private final int visibleSteps;
    private int p1Pos = 0;
    private int p2Pos = 0;
    private int p1Score = 0;
    private int p2Score = 0;
    private int currentPlayer = 1;

    /**
     * Constructor for GameEngine.
     * @param visibleSteps The number of visible steps on the game track.
     */
    public GameEngine(int visibleSteps) {
        this.visibleSteps = visibleSteps;
    }

    /**
     * Updates the current player's position and score when they answer correctly.
     */
    public void onCorrectAnswer() {
        if (currentPlayer == 1) {
            p1Pos++;
            p1Score++;
        } else {
            p2Pos++;
            p2Score++;
        }
    }

    /**
     * Checks if the second player (the chaser) has caught the first player.
     * @return True if player 2's position is greater than or equal to player 1's, false otherwise.
     */
    public boolean isCaught() {
        return currentPlayer == 2 && p2Pos >= p1Pos;
    }

    /**
     * Switches the active player between player 1 and player 2.
     */
    public void switchPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }

    /**
     * Gets the current position of player 1.
     * @return Player 1's position.
     */
    public int getP1Pos() {
        return p1Pos;
    }

    /**
     * Gets the current position of player 2.
     * @return Player 2's position.
     */
    public int getP2Pos() {
        return p2Pos;
    }

    /**
     * Gets the current score of player 1.
     * @return Player 1's score.
     */
    public int getP1Score() {
        return p1Score;
    }

    /**
     * Gets the current score of player 2.
     * @return Player 2's score.
     */
    public int getP2Score() {
        return p2Score;
    }

    /**
     * Gets the ID of the current active player (1 or 2).
     * @return The current player ID.
     */
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Gets the number of visible steps in the game.
     * @return The number of visible steps.
     */
    public int getVisibleSteps() {
        return visibleSteps;
    }
}
