package com.example.chasergame.models;

import android.graphics.Color;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String id;
    public String username;
    public String password;
    public String email;
    public boolean isAdmin;
    public int onlineWins;
    public int botWins; // Total bot wins (can keep for legacy or sum)
    public int botWinsEasy;
    public int botWinsNormal;
    public int botWinsHard;
    public int oneDeviceWins;
    public int points;
    public String equippedSkin = "default";
    public List<String> ownedSkins = new ArrayList<>();
    public boolean giftClaimed = false;
    public int totalRhythmScore = 0; // Cumulative score for the leaderboard
    public String profileImage; // Base64 or URL

    // Custom Skin Settings
    public int customCircleColor = Color.WHITE;
    public int customTargetColor = Color.WHITE;
    public int customBackgroundColor = Color.BLACK;
    public String customEffectType = "none";

    public User() {
        if (ownedSkins == null) ownedSkins = new ArrayList<>();
        if (!ownedSkins.contains("default")) ownedSkins.add("default");
    }

    public User(String id, String username, String password, String email, boolean isAdmin, int onlineWins, int botWins, int oneDeviceWins) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.isAdmin = isAdmin;
        this.onlineWins = onlineWins;
        this.botWins = botWins;
        this.botWinsEasy = 0;
        this.botWinsNormal = 0;
        this.botWinsHard = 0;
        this.oneDeviceWins = oneDeviceWins;
        this.points = 0;
        this.equippedSkin = "default";
        this.ownedSkins = new ArrayList<>();
        this.ownedSkins.add("default");
        this.giftClaimed = false;
        this.totalRhythmScore = 0;

        // Default custom settings
        this.customCircleColor = Color.WHITE;
        this.customTargetColor = Color.WHITE;
        this.customBackgroundColor = Color.BLACK;
        this.customEffectType = "none";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public int getOnlineWins() {
        return onlineWins;
    }

    public void setOnlineWins(int onlineWins) {
        this.onlineWins = onlineWins;
    }

    public int getBotWins() {
        return botWins;
    }

    public void setBotWins(int botWins) {
        this.botWins = botWins;
    }

    public int getBotWinsEasy() {
        return botWinsEasy;
    }

    public void setBotWinsEasy(int botWinsEasy) {
        this.botWinsEasy = botWinsEasy;
    }

    public int getBotWinsNormal() {
        return botWinsNormal;
    }

    public void setBotWinsNormal(int botWinsNormal) {
        this.botWinsNormal = botWinsNormal;
    }

    public int getBotWinsHard() {
        return botWinsHard;
    }

    public void setBotWinsHard(int botWinsHard) {
        this.botWinsHard = botWinsHard;
    }

    public int getOneDeviceWins() {
        return oneDeviceWins;
    }

    public void setOneDeviceWins(int oneDeviceWins) {
        this.oneDeviceWins = oneDeviceWins;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getEquippedSkin() {
        return equippedSkin;
    }

    public void setEquippedSkin(String equippedSkin) {
        this.equippedSkin = equippedSkin;
    }

    public List<String> getOwnedSkins() {
        if (ownedSkins == null) ownedSkins = new ArrayList<>();
        return ownedSkins;
    }

    public void setOwnedSkins(List<String> ownedSkins) {
        this.ownedSkins = ownedSkins;
    }

    public boolean isGiftClaimed() {
        return giftClaimed;
    }

    public void setGiftClaimed(boolean giftClaimed) {
        this.giftClaimed = giftClaimed;
    }

    public int getTotalRhythmScore() {
        return totalRhythmScore;
    }

    public void setTotalRhythmScore(int totalRhythmScore) {
        this.totalRhythmScore = totalRhythmScore;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public int getCustomCircleColor() {
        return customCircleColor;
    }

    public void setCustomCircleColor(int customCircleColor) {
        this.customCircleColor = customCircleColor;
    }

    public int getCustomTargetColor() {
        return customTargetColor;
    }

    public void setCustomTargetColor(int customTargetColor) {
        this.customTargetColor = customTargetColor;
    }

    public int getCustomBackgroundColor() {
        return customBackgroundColor;
    }

    public void setCustomBackgroundColor(int customBackgroundColor) {
        this.customBackgroundColor = customBackgroundColor;
    }

    public String getCustomEffectType() {
        return customEffectType;
    }

    public void setCustomEffectType(String customEffectType) {
        this.customEffectType = customEffectType;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", points=" + points +
                ", equippedSkin='" + equippedSkin + '\'' +
                ", totalRhythmScore=" + totalRhythmScore +
                '}';
    }
}
