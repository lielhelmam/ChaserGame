package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
    
    // Secret navigation logic
    private static final long SECRET_HOLD_DURATION = 1600L; 
    private final Handler secretHandler = new Handler(Looper.getMainLooper());
    private boolean isSecretTriggered = false;
    
    private final Runnable secretRunnable = new Runnable() {
        @Override
        public void run() {
            isSecretTriggered = true;
            Log.d(TAG, "Secret triggered! Navigating to SecretGameRuelsActivity");
            // Fixed: Corrected the class name to match SecretGameRuelsActivity
            Intent intent = new Intent(MainActivity.this, SecretGameRuelsActivity.class);
            startActivity(intent);
        }
    };

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
        Button btnLeaderBoards = findViewById(R.id.btn_main_leaderboards);
        
        // Handle normal click
        btnLeaderBoards.setOnClickListener(v -> {
            if (isSecretTriggered) {
                // If the secret was just triggered, don't open the leaderboard
                isSecretTriggered = false; 
                return;
            }
            Intent intent = new Intent(MainActivity.this, LeaderBoardActivity.class);
            startActivity(intent);
        });

        // Handle long press
        btnLeaderBoards.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isSecretTriggered = false;
                secretHandler.postDelayed(secretRunnable, SECRET_HOLD_DURATION);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                secretHandler.removeCallbacks(secretRunnable);
            }
            return false; // Return false to allow onClick to fire
        });

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

    @Override
    protected void onResume() {
        super.onResume();
        isSecretTriggered = false; // Reset when returning to this screen
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        secretHandler.removeCallbacks(secretRunnable);
    }
}
