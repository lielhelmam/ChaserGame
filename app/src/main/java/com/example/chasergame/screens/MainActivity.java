package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        setupButtons();
        setupNavigationDrawer();
        updateUIWithUserData();
    }

    private void setupNavigationDrawer() {
        // Handle the menu icon click to open the drawer
        findViewById(R.id.btn_open_drawer).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, EditProfileActivity.class));
            } else if (id == R.id.nav_leaderboard) {
                startActivity(new Intent(this, LeaderBoardActivity.class));
            } else if (id == R.id.nav_shop) {
                startActivity(new Intent(this, ShopActivity.class));
            } else if (id == R.id.nav_logout) {
                showLogoutDialog();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void updateUIWithUserData() {
        User user = SharedPreferencesUtil.getUser(this);
        if (user != null) {
            // Update main screen greeting
            TextView hitouser = findViewById(R.id.text_main_hitouser);
            hitouser.setText("Hi, " + user.getUsername());

            // Update Drawer Header
            View headerView = navigationView.getHeaderView(0);
            TextView navUsername = headerView.findViewById(R.id.nav_header_username);
            TextView navEmail = headerView.findViewById(R.id.nav_header_email);

            navUsername.setText(user.getUsername());
            navEmail.setText(user.getEmail());
        }
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
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
