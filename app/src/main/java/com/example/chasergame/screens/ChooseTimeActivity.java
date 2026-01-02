package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;

public class ChooseTimeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_time);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.ChooseTimePage),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        NumberPicker pickerMinutes = findViewById(R.id.pickerMinutes);
        NumberPicker pickerSeconds = findViewById(R.id.pickerSeconds);

        pickerMinutes.setMinValue(0);
        pickerMinutes.setMaxValue(10); // up to 10 minutes

        pickerSeconds.setMinValue(0);
        pickerSeconds.setMaxValue(59);

        findViewById(R.id.btnStartGame).setOnClickListener(v -> {

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
            finish(); // ⭐ חשוב
        });
    }
}
