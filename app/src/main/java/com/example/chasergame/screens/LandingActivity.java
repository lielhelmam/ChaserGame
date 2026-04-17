package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.example.chasergame.R;
import com.example.chasergame.services.DatabaseService;

public class LandingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        hideNavigationDrawer();
        hideTopBar();

        if (authService.isUserLoggedIn()) {
            com.example.chasergame.models.User localUser = authService.getCurrentUser();
            databaseService.getUser(localUser.getId(), new DatabaseService.DatabaseCallback<com.example.chasergame.models.User>() {
                @Override
                public void onCompleted(com.example.chasergame.models.User freshUser) {
                    if (freshUser != null) {
                        authService.syncUser(freshUser);
                    }
                    navigateToNext();
                }

                @Override
                public void onFailed(Exception e) {
                    navigateToNext();
                }
            });
        }

        setupButtons();
    }

    private void navigateToNext() {
        Intent intent = authService.getNextActivityIntent();
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    private void setupButtons() {
        Button btnReg = findViewById(R.id.btn_main_gotosignup);
        if (btnReg != null) {
            btnReg.setOnClickListener(view ->
                    startActivity(new Intent(this, SignupActivity.class)));
        }

        Button btnLog = findViewById(R.id.btn_main_gotologin);
        if (btnLog != null) {
            btnLog.setOnClickListener(view ->
                    startActivity(new Intent(this, LoginActivity.class)));
        }

        Button exitBtn = findViewById(R.id.btn_main_exit);
        if (exitBtn != null) {
            exitBtn.setOnClickListener(v -> showExitDialog());
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    finishAffinity();
                    System.exit(0);
                })
                .setNegativeButton("No", null)
                .show();
    }
}
