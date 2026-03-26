package com.example.chasergame.screens;

import android.os.Bundle;

import com.example.chasergame.R;

public class OnlineMenuActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_menu);
        setTopBarTitle("Online Game");

        findViewById(R.id.btnCreateGame).setOnClickListener(v ->
                navigateTo(ChooseTimeOnlineActivity.class, false));

        findViewById(R.id.btnJoinGame).setOnClickListener(v ->
                navigateTo(JoinGameActivity.class, false));

        // For now, handling public the same as private since we don't have a separate logic yet
        findViewById(R.id.btnCreatePublicGame).setOnClickListener(v ->
                navigateTo(ChooseTimeOnlineActivity.class, false));

        findViewById(R.id.btnJoinPublicGame).setOnClickListener(v ->
                navigateTo(JoinGameActivity.class, false));
    }
}
