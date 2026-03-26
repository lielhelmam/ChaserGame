package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.chasergame.R;

public class ChooseTimeBotActivity extends BaseActivity {
    private NumberPicker pickerMinutes, pickerSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_time);
        setTopBarTitle("Choose Time vs Bot");

        pickerMinutes = findViewById(R.id.pickerMinutes);
        pickerSeconds = findViewById(R.id.pickerSeconds);

        if (pickerMinutes != null) {
            pickerMinutes.setMinValue(0);
            pickerMinutes.setMaxValue(10);
            pickerMinutes.setValue(2);
        }

        if (pickerSeconds != null) {
            pickerSeconds.setMinValue(0);
            pickerSeconds.setMaxValue(59);
            pickerSeconds.setValue(0);
        }

        findViewById(R.id.btnStartGame).setOnClickListener(v -> goNext());
    }

    private void goNext() {
        if (pickerMinutes == null || pickerSeconds == null) return;

        int minutes = pickerMinutes.getValue();
        int seconds = pickerSeconds.getValue();

        long turnTimeMs = (minutes * 60L + seconds) * 1000L;

        if (turnTimeMs <= 0) {
            Toast.makeText(this, "Please choose time", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, ChooseBotDifficultyActivity.class);
        i.putExtra("TURN_TIME_MS", turnTimeMs);
        startActivity(i);
    }
}
