package com.example.chasergame.services;

import com.example.chasergame.models.Note;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BeatmapGenerator {

    public static List<Note> generate(int bpm, long durationMs, String difficulty) {
        List<Note> notes = new ArrayList<>();
        Random random = new Random();

        // Base beat interval
        long beatInterval = 60000 / Math.max(bpm, 60);
        double noteProbability = 0.6;
        double sliderChance = 0.2; 
        double doubleNoteChance = 0.0;
        String diff = difficulty.toLowerCase();

        long[] lastSliderEndTime = new long[2]; // Track last slider end per lane
        lastSliderEndTime[0] = -2000;
        lastSliderEndTime[1] = -2000;

        switch (diff) {
            case "beginner":
                noteProbability = 0.3;
                sliderChance = 0.4; // More sliders for beginners (easier to hold)
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
            // Check if any slider is currently active in any lane
            boolean sliderActive = (t < lastSliderEndTime[0] || t < lastSliderEndTime[1]);

            if (!sliderActive && random.nextDouble() < noteProbability) {
                int mainLane = random.nextInt(2);
                
                if (random.nextDouble() < sliderChance && t > lastSliderEndTime[mainLane] + 1000) {
                    // Create a slider
                    long sliderDuration = beatInterval * (1 + random.nextInt(3)); 
                    notes.add(new Note(t, mainLane, sliderDuration, Note.Type.SLIDER));
                    lastSliderEndTime[mainLane] = t + sliderDuration;
                    // If it's a slider, we DON'T add a double note to keep it easy
                } else {
                    // Normal note
                    notes.add(new Note(t, mainLane, 0, Note.Type.NORMAL));
                    
                    // Add double notes only if the first one was a normal note
                    if (random.nextDouble() < doubleNoteChance) {
                        int otherLane = 1 - mainLane;
                        notes.add(new Note(t, otherLane, 0, Note.Type.NORMAL));
                    }
                }
            }
        }

        return notes;
    }
}
