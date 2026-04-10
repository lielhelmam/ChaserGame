package com.example.chasergame.services;

import com.example.chasergame.models.Note;
import com.example.chasergame.models.SongData;

import java.util.ArrayList;
import java.util.List;

public class SongService {
    private final ISongRepository songRepository;

    /**
     * Constructor for SongService.
     *
     * @param songRepository The repository used to perform CRUD operations on songs.
     */
    public SongService(ISongRepository songRepository) {
        this.songRepository = songRepository;
    }

    /**
     * Retrieves all available songs from the database.
     *
     * @param callback Callback to return the list of SongData objects.
     */
    public void getAllSongs(DatabaseService.DatabaseCallback<List<SongData>> callback) {
        songRepository.getSongList(callback);
    }

    /**
     * Adds a new song to the database with automatic beatmap generation parameters and HP system.
     */
    public void addSong(String name, String difficulty, String resName, int bpm, int hpDrain, int hpGain, DatabaseService.DatabaseCallback<Void> callback) {
        SongData song = new SongData(name, difficulty, 0, null, hpDrain, hpGain);
        song.setResName(resName);
        song.setBpm(bpm);
        songRepository.addSong(song, callback);
    }

    /**
     * Updates an existing song's details in the database including HP parameters.
     */
    public void updateSong(String songId, String name, String difficulty, String resName, int bpm, int hpDrain, int hpGain, DatabaseService.DatabaseCallback<Void> callback) {
        SongData updatedSong = new SongData(name, difficulty, 0, null, hpDrain, hpGain);
        updatedSong.setResName(resName);
        updatedSong.setBpm(bpm);
        songRepository.updateSong(songId, updatedSong, callback);
    }

    /**
     * Deletes a song from the database.
     *
     * @param songId   The unique ID of the song to delete.
     * @param callback Callback to handle the result of the deletion.
     */
    public void deleteSong(String songId, DatabaseService.DatabaseCallback<Void> callback) {
        songRepository.deleteSong(songId, callback);
    }

    /**
     * Parses a beatmap string (format: "timestamp,lane;timestamp,lane") into a list of Note objects.
     *
     * @param beatmapStr The beatmap string to parse.
     * @return A list of Note objects, or an empty list if parsing fails.
     */
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

    /**
     * Converts a list of Note objects into a formatted beatmap string.
     *
     * @param notes The list of notes.
     * @return A string representation of the beatmap.
     */
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
