package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.chasergame.R;

public class SecretGameRulesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_game_rules);
        setTopBarTitle("Rhythm Game Rules");

        Button btnStartGame = findViewById(R.id.btn_start_secret_game);
        btnStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(SecretGameRulesActivity.this, SongSelectionActivity.class);
            startActivity(intent);
            finish();
        });

        Button btnBackToMain = findViewById(R.id.btn_back_to_main);
        btnBackToMain.setOnClickListener(v -> {
            finish();
        });
    }
}
