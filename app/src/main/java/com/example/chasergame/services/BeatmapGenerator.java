package com.example.chasergame.services;

import com.example.chasergame.models.Note;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BeatmapGenerator {

    public static List<Note> generate(int bpm, long durationMs, String difficulty) {
        return generate(bpm, durationMs, difficulty, new ArrayList<>());
    }

    public static List<Note> generate(int bpm, long durationMs, String difficulty, List<String> mods) {
        List<Note> notes = new ArrayList<>();
        Random random = new Random();

        if (mods == null) mods = new ArrayList<>();

        // Overclocked mod: Speed up the BPM
        if (mods.contains("OVERCLOCK")) {
            bpm = (int)(bpm * 1.25);
        }

        // Base beat interval
        long beatInterval = 60000 / Math.max(bpm, 60);
        double noteProbability = 0.6;
        double sliderChance = 0.2;
        double doubleNoteChance = 0.0;
        String diff = difficulty.toLowerCase();

        long[] lastSliderEndTime = new long[2]; 
        lastSliderEndTime[0] = -2000;
        lastSliderEndTime[1] = -2000;

        switch (diff) {
            case "beginner":
                noteProbability = 0.3;
                sliderChance = 0.4;
                beatInterval *= 2;
                break;
            case "easy":
                noteProbability = 0.4;
                sliderChance = 0.3;
                break;
            case "medium":
                noteProbability = 0.6;
                sliderChance = 0.25;
                break;
            case "hard":
                noteProbability = 0.7;
                sliderChance = 0.35;
                beatInterval /= 2;
                doubleNoteChance = 0.1;
                break;
            case "insane":
                noteProbability = 0.8;
                sliderChance = 0.45;
                beatInterval /= 2;
                doubleNoteChance = 0.15;
                break;
            case "expert":
                noteProbability = 0.9;
                sliderChance = 0.55;
                beatInterval /= 2;
                doubleNoteChance = 0.2;
                break;
        }

        // Generate notes
        for (long t = 2500; t < durationMs - 5000; t += beatInterval) {

            // SPINNER LOGIC
            if (Math.abs(t - (durationMs * 0.4)) < beatInterval / 2 ||
                    Math.abs(t - (durationMs * 0.85)) < beatInterval / 2) {

                long spinnerDuration = 3000;
                notes.add(new Note(t, 0, spinnerDuration, Note.Type.SPINNER));
                t += spinnerDuration + 1000;
                continue;
            }

            boolean sliderActive = (t < lastSliderEndTime[0] || t < lastSliderEndTime[1]);

            if (!sliderActive && random.nextDouble() < noteProbability) {
                int mainLane = random.nextInt(2);

                if (random.nextDouble() < sliderChance && t > lastSliderEndTime[mainLane] + 1000) {
                    long sliderDuration = beatInterval * (1 + random.nextInt(3));
                    notes.add(new Note(t, mainLane, sliderDuration, Note.Type.SLIDER));
                    lastSliderEndTime[mainLane] = t + sliderDuration;
                } else {
                    notes.add(new Note(t, mainLane, 0, Note.Type.NORMAL));

                    // DUAL STREAM MOD: Always add second note
                    if (mods.contains("DUAL") || (random.nextDouble() < doubleNoteChance)) {
                        int otherLane = 1 - mainLane;
                        notes.add(new Note(t, otherLane, 0, Note.Type.NORMAL));
                    }
                }
            }
        }

        // GRAVITY WARP MOD: Randomly shift timestamps
        if (mods.contains("GRAVITY")) {
            for (Note n : notes) {
                if (n.getType() != Note.Type.SPINNER) {
                    long shift = (long)((random.nextDouble() - 0.5) * 250); 
                    n.setTimestamp(n.getTimestamp() + shift);
                }
            }
        }

        return notes;
    }
}
