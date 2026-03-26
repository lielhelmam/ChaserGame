package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;

import com.example.chasergame.R;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // BaseActivity will handle the Drawer and Top Bar
        setContentView(R.layout.activity_main);
        
        // BaseActivity already sets the title to "Hi, Username"
        setupButtons();
    }

    private void setupButtons() {
        findViewById(R.id.btn_main_leaderboards).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LeaderBoardActivity.class));
        });

        findViewById(R.id.btn_main_rythmgame).setOnClickListener(v ->
                startActivity(new Intent(this, SecretGameRulesActivity.class)));

        findViewById(R.id.btn_main_playagainstabot).setOnClickListener(v -> 
            startActivity(new Intent(this, ChooseTimeBotActivity.class)));

        findViewById(R.id.btn_main_playonline).setOnClickListener(v -> 
            startActivity(new Intent(this, OnlineMenuActivity.class)));

        findViewById(R.id.btn_main_playononedevice).setOnClickListener(v -> 
            startActivity(new Intent(this, ChooseTimeActivity.class)));

        findViewById(R.id.btn_main_edit_profile).setOnClickListener(v -> 
            startActivity(new Intent(this, EditProfileActivity.class)));
    }
}
