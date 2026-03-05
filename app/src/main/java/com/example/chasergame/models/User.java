package com.example.chasergame.models;

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
    public int botWins;
    public int oneDeviceWins;
    public int points; // Currency for the shop
    public String equippedSkin = "default"; // ID of the current skin
    public List<String> ownedSkins = new ArrayList<>(); // List of skin IDs owned by user


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
        this.oneDeviceWins = oneDeviceWins;
        this.points = 0;
        this.equippedSkin = "default";
        this.ownedSkins = new ArrayList<>();
        this.ownedSkins.add("default");
    }

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

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", isAdmin=" + isAdmin +
                ", onlineWins=" + onlineWins +
                ", botWins=" + botWins +
                ", oneDeviceWins=" + oneDeviceWins +
                ", points=" + points +
                ", equippedSkin='" + equippedSkin + '\'' +
                ", ownedSkins=" + ownedSkins +
                '}';
    }
}
