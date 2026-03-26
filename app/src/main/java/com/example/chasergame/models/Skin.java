package com.example.chasergame.models;

public class Skin {
    public String id;
    public String name;
    public int price;
    public int circleColor;
    public int targetColor;
    public int backgroundColor;
    public String effectType; // "none", "glow", "particles", etc.

    public Skin() {
    }

    public Skin(String id, String name, int price, int circleColor, int targetColor, int backgroundColor, String effectType) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.circleColor = circleColor;
        this.targetColor = targetColor;
        this.backgroundColor = backgroundColor;
        this.effectType = effectType;
    }
}
