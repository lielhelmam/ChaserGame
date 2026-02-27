package com.example.chasergame.screens;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chasergame.R;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.SongData;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ManageSongsActivity extends BaseActivity {

    private EditText etName, etDifficulty, etResId, etTargetScore, etBeatmap;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_songs);

        mDatabase = FirebaseDatabase.getInstance().getReference("rhythm_songs");

        etName = findViewById(R.id.et_song_name);
        etDifficulty = findViewById(R.id.et_song_difficulty);
        etResId = findViewById(R.id.et_song_res_id);
        etTargetScore = findViewById(R.id.et_target_score);
        etBeatmap = findViewById(R.id.et_beatmap);

        Button btnSave = findViewById(R.id.btn_save_song);
        btnSave.setOnClickListener(v -> saveSong());
    }

    private void saveSong() {
        String name = etName.getText().toString().trim();
        String difficulty = etDifficulty.getText().toString().trim();
        String resName = etResId.getText().toString().trim();
        String scoreStr = etTargetScore.getText().toString().trim();
        String beatmapStr = etBeatmap.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(difficulty) || TextUtils.isEmpty(resName) || 
            TextUtils.isEmpty(scoreStr) || TextUtils.isEmpty(beatmapStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int targetScore = Integer.parseInt(scoreStr);
        List<Note> notes = parseBeatmap(beatmapStr);
        
        if (notes.isEmpty()) {
            Toast.makeText(this, "Invalid beatmap format", Toast.LENGTH_SHORT).show();
            return;
        }

        String songId = mDatabase.push().getKey();
        if (songId == null) return;

        // Create SongData and set the resource name correctly
        SongData song = new SongData(name, difficulty, 0, notes, targetScore);
        song.setResName(resName); // FIX: Setting the resource name for Firebase

        mDatabase.child(songId).setValue(song)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(ManageSongsActivity.this, "Song saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(ManageSongsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private List<Note> parseBeatmap(String beatmapStr) {
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
}
