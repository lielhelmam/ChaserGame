package com.example.chasergame.services;

import com.example.chasergame.models.Note;
import com.example.chasergame.models.SongData;

import java.util.ArrayList;
import java.util.List;

public class SongService {
    private final ISongRepository songRepository;

    public SongService(ISongRepository songRepository) {
        this.songRepository = songRepository;
    }

    public void getAllSongs(DatabaseService.DatabaseCallback<List<SongData>> callback) {
        songRepository.getSongList(callback);
    }

    public void addSong(String name, String difficulty, String resName, int targetScore, String beatmapStr, DatabaseService.DatabaseCallback<Void> callback) {
        List<Note> notes = parseBeatmap(beatmapStr);
        if (notes.isEmpty()) {
            callback.onFailed(new Exception("Invalid beatmap format"));
            return;
        }

        SongData song = new SongData(name, difficulty, 0, notes, targetScore);
        song.setResName(resName);
        songRepository.addSong(song, callback);
    }

    public void updateSong(String songId, String name, String difficulty, String resName, int targetScore, String beatmapStr, DatabaseService.DatabaseCallback<Void> callback) {
        List<Note> notes = parseBeatmap(beatmapStr);
        if (notes.isEmpty()) {
            callback.onFailed(new Exception("Invalid beatmap format"));
            return;
        }

        SongData updatedSong = new SongData(name, difficulty, 0, notes, targetScore);
        updatedSong.setResName(resName);
        songRepository.updateSong(songId, updatedSong, callback);
    }

    public void deleteSong(String songId, DatabaseService.DatabaseCallback<Void> callback) {
        songRepository.deleteSong(songId, callback);
    }

    public List<Note> parseBeatmap(String beatmapStr) {
        List<Note> notes = new ArrayList<>();
        try {
            String[] entries = beatmapStr.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    long time = Long.parseLong(parts[0].trim());
                    int lane = Integer.parseInt(parts[1].trim());
                    notes.add(new Note(time, lane));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notes;
    }

    public String notesToString(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sb.append(note.getTimestamp()).append(",").append(note.getLane());
            if (i < notes.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }
}
