package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.chasergame.R;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        hideNavigationDrawer();
        hideTopBar();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (authService.isUserLoggedIn()) {
                Intent intent = authService.getNextActivityIntent();
                if (intent != null) {
                    startActivity(intent);
                    finish();
                    return;
                }
            }
            navigateTo(LandingActivity.class, true);
        }, SPLASH_DELAY);
    }
}
