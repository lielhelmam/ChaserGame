package com.example.chasergame.screens;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chasergame.R;
import com.example.chasergame.services.DatabaseService;

public class ManageSongsActivity extends BaseActivity {

    private EditText etName, etDifficulty, etResId, etTargetScore, etBeatmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_songs);

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

        int targetScore;
        try {
            targetScore = Integer.parseInt(scoreStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid score", Toast.LENGTH_SHORT).show();
            return;
        }

        songService.addSong(name, difficulty, resName, targetScore, beatmapStr, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(ManageSongsActivity.this, "Song saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageSongsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
