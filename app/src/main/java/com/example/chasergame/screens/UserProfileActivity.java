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
    private TextView tvName, tvEmail, tvPassword, tvAdmin;
    private EditText etOnlineWins, etBotWinsEasy, etBotWinsNormal, etBotWinsHard, etRhythmScore, etPoints;
    private View btnSave;
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

        etOnlineWins = findViewById(R.id.et_online_wins);
        etBotWinsEasy = findViewById(R.id.et_bot_wins_easy);
        etBotWinsNormal = findViewById(R.id.et_bot_wins_normal);
        etBotWinsHard = findViewById(R.id.et_bot_wins_hard);
        etRhythmScore = findViewById(R.id.et_rhythm_score);
        etPoints = findViewById(R.id.et_profile_points);
        btnSave = findViewById(R.id.btn_save_user_changes);

        btnSave.setOnClickListener(v -> saveAllChanges());
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
                
                // Only show editing for admins
                User loggedInUser = authService.getCurrentUser();
                boolean isAdmin = loggedInUser != null && loggedInUser.isAdmin();
                
                etOnlineWins.setEnabled(isAdmin);
                etBotWinsEasy.setEnabled(isAdmin);
                etBotWinsNormal.setEnabled(isAdmin);
                etBotWinsHard.setEnabled(isAdmin);
                etRhythmScore.setEnabled(isAdmin);
                etPoints.setEnabled(isAdmin);
                
                btnSave.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
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

        etOnlineWins.setText(String.valueOf(user.getOnlineWins()));
        etBotWinsEasy.setText(String.valueOf(user.getBotWinsEasy()));
        etBotWinsNormal.setText(String.valueOf(user.getBotWinsNormal()));
        etBotWinsHard.setText(String.valueOf(user.getBotWinsHard()));
        etRhythmScore.setText(String.valueOf(user.getTotalRhythmScore()));
        etPoints.setText(String.valueOf(user.getPoints()));
    }

    private void saveAllChanges() {
        if (currentUser == null) return;

        try {
            currentUser.setOnlineWins(Integer.parseInt(etOnlineWins.getText().toString().trim()));
            currentUser.setBotWinsEasy(Integer.parseInt(etBotWinsEasy.getText().toString().trim()));
            currentUser.setBotWinsNormal(Integer.parseInt(etBotWinsNormal.getText().toString().trim()));
            currentUser.setBotWinsHard(Integer.parseInt(etBotWinsHard.getText().toString().trim()));
            currentUser.setTotalRhythmScore(Integer.parseInt(etRhythmScore.getText().toString().trim()));
            currentUser.setPoints(Integer.parseInt(etPoints.getText().toString().trim()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseService.updateUser(currentUser, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(UserProfileActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                showUserData(currentUser);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
