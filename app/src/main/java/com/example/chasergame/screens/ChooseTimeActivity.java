package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.chasergame.R;

public class ChooseTimeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_time);
        setTopBarTitle("Choose Time 1v1");

        NumberPicker pickerMinutes = findViewById(R.id.pickerMinutes);
        NumberPicker pickerSeconds = findViewById(R.id.pickerSeconds);

        if (pickerMinutes != null) {
            pickerMinutes.setMinValue(0);
            pickerMinutes.setMaxValue(10);
        }

        if (pickerSeconds != null) {
            pickerSeconds.setMinValue(0);
            pickerSeconds.setMaxValue(59);
        }

        findViewById(R.id.btnStartGame).setOnClickListener(v -> {
            if (pickerMinutes == null || pickerSeconds == null) return;

            int minutes = pickerMinutes.getValue();
            int seconds = pickerSeconds.getValue();

            if (minutes == 0 && seconds == 0) {
                Toast.makeText(this, "Please choose a valid time", Toast.LENGTH_SHORT).show();
                return;
            }

            long totalMillis = (minutes * 60L + seconds) * 1000L;

            Intent intent = new Intent(
                    ChooseTimeActivity.this,
                    PlayOnOneDeviceActivity.class
            );
            intent.putExtra("TURN_TIME_MS", totalMillis);

            startActivity(intent);
            finish();
        });
    }
}
