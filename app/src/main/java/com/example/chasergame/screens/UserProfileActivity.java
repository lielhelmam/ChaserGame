package com.example.chasergame.screens;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;

public class UserProfileActivity extends BaseActivity {
    private TextView tvName, tvEmail, tvPassword, tvAdmin, tvOnlineWins, tvBotWinsEasy, tvBotWinsNormal, tvBotWinsHard, tvRhythmScore;
    private ProgressBar progressBar;

    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        userId = getIntent().getStringExtra("USER_UID");
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "No user specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUser();
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPassword = findViewById(R.id.tv_profile_password);
        tvAdmin = findViewById(R.id.tv_profile_admin);
        progressBar = findViewById(R.id.profile_progress);

        tvOnlineWins = findViewById(R.id.tv_online_wins);
        tvBotWinsEasy = findViewById(R.id.tv_bot_wins_easy);
        tvBotWinsNormal = findViewById(R.id.tv_bot_wins_normal);
        tvBotWinsHard = findViewById(R.id.tv_bot_wins_hard);
        tvRhythmScore = findViewById(R.id.tv_rhythm_score);

        findViewById(R.id.btn_edit_online_wins).setOnClickListener(v -> showEditValueDialog("onlineWins", currentUser.getOnlineWins()));
        findViewById(R.id.btn_edit_bot_wins_easy).setOnClickListener(v -> showEditValueDialog("botWinsEasy", currentUser.getBotWinsEasy()));
        findViewById(R.id.btn_edit_bot_wins_normal).setOnClickListener(v -> showEditValueDialog("botWinsNormal", currentUser.getBotWinsNormal()));
        findViewById(R.id.btn_edit_bot_wins_hard).setOnClickListener(v -> showEditValueDialog("botWinsHard", currentUser.getBotWinsHard()));
        findViewById(R.id.btn_edit_rhythm_score).setOnClickListener(v -> showEditValueDialog("totalRhythmScore", currentUser.getTotalRhythmScore()));
    }

    private void loadUser() {
        progressBar.setVisibility(View.VISIBLE);
        databaseService.getUser(userId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(User user) {
                progressBar.setVisibility(View.GONE);
                if (user == null) {
                    Toast.makeText(UserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentUser = user;
                showUserData(user);
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UserProfileActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserData(User user) {
        tvName.setText(user.getUsername() == null ? "-" : user.getUsername());
        tvEmail.setText(user.getEmail() == null ? "-" : user.getEmail());
        tvPassword.setText(user.getPassword() == null ? "-" : user.getPassword());
        tvAdmin.setText((user.isAdmin() ? "Yes" : "No"));

        tvOnlineWins.setText(String.valueOf(user.getOnlineWins()));
        tvBotWinsEasy.setText(String.valueOf(user.getBotWinsEasy()));
        tvBotWinsNormal.setText(String.valueOf(user.getBotWinsNormal()));
        tvBotWinsHard.setText(String.valueOf(user.getBotWinsHard()));
        tvRhythmScore.setText(String.valueOf(user.getTotalRhythmScore()));
    }

    private void showEditValueDialog(String field, int currentVal) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentVal));

        new AlertDialog.Builder(this)
                .setTitle("Edit " + field)
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString();
                    if (!value.isEmpty()) updateValue(field, Integer.parseInt(value));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateValue(String field, int newValue) {
        if (currentUser == null) return;

        switch (field) {
            case "onlineWins":
                currentUser.setOnlineWins(newValue);
                break;
            case "botWinsEasy":
                currentUser.setBotWinsEasy(newValue);
                break;
            case "botWinsNormal":
                currentUser.setBotWinsNormal(newValue);
                break;
            case "botWinsHard":
                currentUser.setBotWinsHard(newValue);
                break;
            case "totalRhythmScore":
                currentUser.setTotalRhythmScore(newValue);
                break;
        }

        databaseService.updateUser(currentUser, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(UserProfileActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                showUserData(currentUser);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
