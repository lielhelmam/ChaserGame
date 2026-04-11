package com.example.chasergame.screens;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.chasergame.R;
import com.example.chasergame.services.DatabaseService;

public class ManageSongsActivity extends BaseActivity {

    private final String[] difficultyLevels = {
            "Beginner (40-90)",
            "Easy (80-120)",
            "Medium (100-150)",
            "Hard (130-180)",
            "Insane (160-220)",
            "Expert (200-300)"
    };
    private EditText etName, etResId, etBpm, etHpDrain, etHpGain;
    private Spinner spDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_songs);

        etName = findViewById(R.id.et_song_name);
        spDifficulty = findViewById(R.id.sp_song_difficulty);
        etResId = findViewById(R.id.et_song_res_id);
        etBpm = findViewById(R.id.et_song_bpm);
        etHpDrain = findViewById(R.id.et_hp_drain);
        etHpGain = findViewById(R.id.et_hp_gain);

        setupSpinner();

        Button btnSave = findViewById(R.id.btn_save_song);
        btnSave.setOnClickListener(v -> saveSong());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, difficultyLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDifficulty.setAdapter(adapter);
    }

    private void saveSong() {
        String name = etName.getText().toString().trim();
        String selectedDifficulty = spDifficulty.getSelectedItem().toString();
        // Extract base difficulty name (everything before the first space)
        String difficulty = selectedDifficulty.split(" ")[0];

        String resName = etResId.getText().toString().trim();
        String bpmStr = etBpm.getText().toString().trim();
        String drainStr = etHpDrain.getText().toString().trim();
        String gainStr = etHpGain.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(resName) ||
                TextUtils.isEmpty(bpmStr) || TextUtils.isEmpty(drainStr) || TextUtils.isEmpty(gainStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int bpm, hpDrain, hpGain;
        try {
            bpm = Integer.parseInt(bpmStr);
            hpDrain = Integer.parseInt(drainStr);
            hpGain = Integer.parseInt(gainStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate BPM based on difficulty
        if (!isBpmValidForDifficulty(bpm, difficulty)) {
            return;
        }

        songService.addSong(name, difficulty, resName, bpm, hpDrain, hpGain, new DatabaseService.DatabaseCallback<Void>() {
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

    private boolean isBpmValidForDifficulty(int bpm, String difficulty) {
        int min, max;
        switch (difficulty.toLowerCase()) {
            case "beginner":
                min = 40;
                max = 90;
                break;
            case "easy":
                min = 80;
                max = 120;
                break;
            case "medium":
                min = 100;
                max = 150;
                break;
            case "hard":
                min = 130;
                max = 180;
                break;
            case "insane":
                min = 160;
                max = 220;
                break;
            case "expert":
                min = 200;
                max = 300;
                break;
            default:
                min = 40;
                max = 300;
        }
        if (bpm < min || bpm > max) {
            Toast.makeText(this, "BPM for " + difficulty + " must be between " + min + " and " + max, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
