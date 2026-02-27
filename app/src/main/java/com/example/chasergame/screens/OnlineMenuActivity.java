package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;

import com.example.chasergame.R;

public class OnlineMenuActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_menu);

        findViewById(R.id.btnCreateGame).setOnClickListener(v -> startActivity(new Intent(this, ChooseTimeOnlineActivity.class)));

        findViewById(R.id.btnJoinGame).setOnClickListener(v -> startActivity(new Intent(this, JoinGameActivity.class)));
    }
}
