package com.example.chasergame.models;

public class GameResult {
    private String userId;
    private String username;
    private String gameMode;
    private boolean win;

    public GameResult(String userId, String username, String gameMode, boolean win) {
        this.userId = userId;
        this.username = username;
        this.gameMode = gameMode;
        this.win = win;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getGameMode() {
        return gameMode;
    }

    public boolean isWin() {
        return win;
    }
}