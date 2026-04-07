package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;

import com.example.chasergame.R;

public class MainActivity extends BaseActivity {

    /**
     * Called when the activity is first created.
     * Sets the layout and initializes the main screen's buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // BaseActivity will handle the Drawer and Top Bar
        setContentView(R.layout.activity_main);

        // BaseActivity already sets the title to "Hi, Username"
        setupButtons();
    }

    /**
     * Sets up the click listeners for all buttons on the main dashboard.
     * Links each button to its corresponding activity (Leaderboards, Rhythm Game, Bot Play, etc.).
     */
    private void setupButtons() {
        findViewById(R.id.btn_main_leaderboards).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LeaderBoardActivity.class)));

        findViewById(R.id.btn_main_rythmgame).setOnClickListener(v ->
                startActivity(new Intent(this, RhythmGameRulesActivity.class)));

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
