package com.example.chasergame.models;

public class User {
    public String id;
    public String username;
    public String password;
    public String email;
    public boolean isAdmin;
    public int onlineWins;
    public int botWins;
    public int oneDeviceWins;


    public User() {
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
                '}';
    }
}
