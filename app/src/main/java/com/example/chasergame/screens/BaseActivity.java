package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.google.android.material.navigation.NavigationView;

public class BaseActivity extends AppCompatActivity {

    protected DatabaseService databaseService;
    protected DrawerLayout drawerLayout;
    private final OnBackPressedCallback drawerBackCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        }
    };
    protected NavigationView navigationView;
    private FrameLayout contentFrame;
    private TextView topBarTitle;
    private View topBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseService = DatabaseService.getInstance();
        getOnBackPressedDispatcher().addCallback(this, drawerBackCallback);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        contentFrame = drawerLayout.findViewById(R.id.content_frame);
        topBarTitle = drawerLayout.findViewById(R.id.text_top_bar_title);
        navigationView = drawerLayout.findViewById(R.id.nav_view);
        topBar = drawerLayout.findViewById(R.id.top_bar);

        getLayoutInflater().inflate(layoutResID, contentFrame, true);
        super.setContentView(drawerLayout);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                drawerBackCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                drawerBackCallback.setEnabled(false);
            }
        });

        setupBaseNavigation();
    }

    private void setupBaseNavigation() {
        if (navigationView == null) return;

        View btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                if (!(this instanceof MainActivity)) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            } else if (id == R.id.nav_profile) {
                if (!(this instanceof EditProfileActivity)) {
                    startActivity(new Intent(this, EditProfileActivity.class));
                }
            } else if (id == R.id.nav_leaderboard) {
                if (!(this instanceof LeaderBoardActivity)) {
                    startActivity(new Intent(this, LeaderBoardActivity.class));
                }
            } else if (id == R.id.nav_shop) {
                if (!(this instanceof ShopActivity)) {
                    startActivity(new Intent(this, ShopActivity.class));
                }
            } else if (id == R.id.nav_logout) {
                showLogoutDialog();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        updateHeaderData();
    }

    private void updateHeaderData() {
        User user = SharedPreferencesUtil.getUser(this);
        if (user != null && navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView navUsername = headerView.findViewById(R.id.nav_header_username);
                TextView navEmail = headerView.findViewById(R.id.nav_header_email);

                if (navUsername != null) navUsername.setText(user.getUsername());
                if (navEmail != null) navEmail.setText(user.getEmail());
            }

            if (topBarTitle != null) {
                topBarTitle.setText("Hi, " + user.getUsername());
            }
        }
    }

    /**
     * Call this in onCreate() AFTER setContentView() to hide the navigation drawer
     */
    protected void hideNavigationDrawer() {
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        View btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        if (btnOpenDrawer != null) {
            btnOpenDrawer.setVisibility(View.GONE);
        }
    }

    /**
     * Call this in onCreate() AFTER setContentView() to hide the top bar
     */
    protected void hideTopBar() {
        if (topBar != null) {
            topBar.setVisibility(View.GONE);
        }
    }

    protected void setTopBarTitle(String title) {
        if (topBarTitle != null) {
            topBarTitle.setText(title);
        }
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
