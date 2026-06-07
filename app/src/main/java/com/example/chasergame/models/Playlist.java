package com.example.chasergame.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private String id;
    private String name;
    private List<String> songIds;

    public Playlist() {
        this.songIds = new ArrayList<>();
    }

    public Playlist(String id, String name, List<String> songIds) {
        this.id = id;
        this.name = name;
        this.songIds = songIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSongIds() {
        if (songIds == null) songIds = new ArrayList<>();
        return songIds;
    }

    public void setSongIds(List<String> songIds) {
        this.songIds = songIds;
    }
}
