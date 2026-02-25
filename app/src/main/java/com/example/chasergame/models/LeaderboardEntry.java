package com.example.chasergame.models;

public class LeaderboardEntry {
    private int rank;
    private String username;
    private int score;

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