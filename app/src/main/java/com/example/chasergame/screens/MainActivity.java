package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.utils.SharedPreferencesUtil;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private Button Logout, PlayAgainstBot, PlayOnOneDevice, btnEditProfile, btnPlayOnline, btnLeaderBoards;
    private TextView hitouser;
    private User user;

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
        btnLeaderBoards = findViewById(R.id.btn_main_leaderboards);
        btnLeaderBoards.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, LeaderBoardActivity.class);
            startActivity(intent);
        });

        PlayAgainstBot = findViewById(R.id.btn_main_playagainstabot);
        PlayAgainstBot.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ChooseTimeBotActivity.class);
            startActivity(intent);
        });

        btnPlayOnline = findViewById(R.id.btn_main_playonline);
        btnPlayOnline.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OnlineMenuActivity.class);
            startActivity(intent);
        });
        PlayOnOneDevice = findViewById(R.id.btn_main_playononedevice);
        PlayOnOneDevice.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ChooseTimeActivity.class);
            startActivity(intent);
        });

        btnEditProfile = findViewById(R.id.btn_main_edit_profile);
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        Logout = findViewById(R.id.btn_main_logout);
        Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        hitouser = findViewById(R.id.text_main_hitouser);
        user = SharedPreferencesUtil.getUser(this);
        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            hitouser.setText("hi " + user.getUsername());
        }

    } // oncreate function

    private void signOut() {
        Log.d(TAG, "Sign out button clicked");
        SharedPreferencesUtil.signOutUser(MainActivity.this);

        Log.d(TAG, "User signed out, redirecting to LandingActivity");
        Intent landingIntent = new Intent(MainActivity.this, LandingActivity.class);
        landingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(landingIntent);
    }
}// end of class