package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;

public class ChooseBotDifficultyActivity extends BaseActivity {

    private long turnTimeMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_bot_difficulty);

        turnTimeMs = getIntent().getLongExtra("TURN_TIME_MS", 120_000);
    }

    public void easy(View v) {
        startGame(50);
    }

    public void normal(View v) {
        startGame(70);
    }

    public void hard(View v) {
        startGame(90);
    }

    private void startGame(int accuracy) {
        Intent i = new Intent(this, PlayAgainstBotActivity.class);
        i.putExtra("TURN_TIME_MS", turnTimeMs);
        i.putExtra("BOT_ACCURACY", accuracy);
        startActivity(i);
        finish();
    }
}
