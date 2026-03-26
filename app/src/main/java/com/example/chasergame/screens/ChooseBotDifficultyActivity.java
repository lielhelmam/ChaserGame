package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;

import com.example.chasergame.R;

public class ChooseBotDifficultyActivity extends BaseActivity {

    private long turnTimeMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_bot_difficulty);

        turnTimeMs = getIntent().getLongExtra("TURN_TIME_MS", 120_000);

        findViewById(R.id.btn_easy).setOnClickListener(v -> startGame(35));
        findViewById(R.id.btn_normal).setOnClickListener(v -> startGame(55));
        findViewById(R.id.btn_hard).setOnClickListener(v -> startGame(75));
    }

    private void startGame(int accuracy) {
        Intent i = new Intent(this, PlayAgainstBotActivity.class);
        i.putExtra("TURN_TIME_MS", turnTimeMs);
        i.putExtra("BOT_ACCURACY", accuracy);
        startActivity(i);
        finish();
    }
}
