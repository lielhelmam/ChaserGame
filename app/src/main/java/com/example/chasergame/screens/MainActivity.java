package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.utils.SharedPreferencesUtil;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.MainPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();
        
        TextView hitouser = findViewById(R.id.text_main_hitouser);
        User user = SharedPreferencesUtil.getUser(this);
        if (SharedPreferencesUtil.isUserLoggedIn(this) && user != null) {
            hitouser.setText("hi " + user.getUsername());
        }
    }

    private void setupButtons() {
        findViewById(R.id.btn_main_leaderboards).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LeaderBoardActivity.class));
        });

        // FIXED: Navigate to the Rules activity first (using the correct spelling from Manifest)
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

        findViewById(R.id.btn_main_logout).setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void signOut() {
        SharedPreferencesUtil.signOutUser(this);
        Intent intent = new Intent(this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
