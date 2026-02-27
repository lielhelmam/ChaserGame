package com.example.chasergame.models;

public class LeaderboardEntry {
    private final String username;
    private final int score;
    private int rank;

    public LeaderboardEntry(int rank, String username, int score) {
        this.rank = rank;
        this.username = username;
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }
}