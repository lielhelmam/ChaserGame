package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.example.chasergame.services.AuthService;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.services.LeaderboardService;
import com.example.chasergame.services.QuestionService;
import com.example.chasergame.services.ShopService;
import com.example.chasergame.services.SongService;
import com.example.chasergame.utils.ImageUtils;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected DatabaseService databaseService;
    protected AuthService authService;
    protected QuestionService questionService;
    protected SongService songService;
    protected LeaderboardService leaderboardService;
    protected ShopService shopService;
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
        authService = new AuthService(this, databaseService);
        questionService = new QuestionService(databaseService);
        songService = new SongService(databaseService);
        leaderboardService = new LeaderboardService(databaseService);
        shopService = new ShopService(authService);
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

        if (drawerLayout != null) {
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
        }

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
                navigateTo(MainActivity.class, true);
            } else if (id == R.id.nav_profile) {
                navigateTo(EditProfileActivity.class, false);
            } else if (id == R.id.nav_leaderboard) {
                navigateTo(LeaderBoardActivity.class, false);
            } else if (id == R.id.nav_shop) {
                navigateTo(ShopActivity.class, false);
            } else if (id == R.id.nav_admin) {
                navigateTo(AdminActivity.class, false);
            } else if (id == R.id.nav_logout) {
                showLogoutDialog();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        updateHeaderAndMenu();
    }

    protected void navigateTo(Class<?> cls, boolean finishCurrent) {
        if (!this.getClass().equals(cls)) {
            startActivity(new Intent(this, cls));
            if (finishCurrent) finish();
        }
    }

    private void updateHeaderAndMenu() {
        User user = authService.getCurrentUser();
        if (user != null && navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView navUsername = headerView.findViewById(R.id.nav_header_username);
                TextView navEmail = headerView.findViewById(R.id.nav_header_email);
                ImageView navImage = headerView.findViewById(R.id.nav_header_image);

                if (navUsername != null) navUsername.setText(user.getUsername());
                if (navEmail != null) navEmail.setText(user.getEmail());
                if (navImage != null && user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                    navImage.setImageBitmap(ImageUtils.decodeImage(user.getProfileImage()));
                }
            }

            Menu menu = navigationView.getMenu();
            MenuItem adminItem = menu.findItem(R.id.nav_admin);
            if (adminItem != null) {
                adminItem.setVisible(user.isAdmin());
            }

            if (topBarTitle != null) {
                topBarTitle.setText("Hi, " + user.getUsername());
            }
        }
    }

    protected void hideNavigationDrawer() {
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        View btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        if (btnOpenDrawer != null) {
            btnOpenDrawer.setVisibility(View.GONE);
        }
    }

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

    protected void signOut() {
        authService.logout();
        Intent intent = new Intent(this, LandingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
