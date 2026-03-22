package com.example.chasergame.screens;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private static final String TAG = "UserProfileActivity";

    private TextView tvName, tvEmail, tvPassword, tvAdmin, tvOnlineWins, tvBotWinsEasy, tvBotWinsNormal, tvBotWinsHard, tvOneDeviceWins;
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

        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPassword = findViewById(R.id.tv_profile_password);
        tvAdmin = findViewById(R.id.tv_profile_admin);
        progressBar = findViewById(R.id.profile_progress);
        
        tvOnlineWins = findViewById(R.id.tv_online_wins);
        tvBotWinsEasy = findViewById(R.id.tv_bot_wins_easy);
        tvBotWinsNormal = findViewById(R.id.tv_bot_wins_normal);
        tvBotWinsHard = findViewById(R.id.tv_bot_wins_hard);
        tvOneDeviceWins = findViewById(R.id.tv_one_device_wins);

        Button btnEditOnlineWins = findViewById(R.id.btn_edit_online_wins);
        Button btnEditBotEasy = findViewById(R.id.btn_edit_bot_wins_easy);
        Button btnEditBotNormal = findViewById(R.id.btn_edit_bot_wins_normal);
        Button btnEditBotHard = findViewById(R.id.btn_edit_bot_wins_hard);
        Button btnEditOneDeviceWins = findViewById(R.id.btn_edit_one_device_wins);

        userId = getIntent().getStringExtra("USER_UID");
        if (userId == null || userId.trim().isEmpty()) {
            Log.e(TAG, "No USER_UID passed to activity");
            Toast.makeText(this, "No user specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUser();

        btnEditOnlineWins.setOnClickListener(v -> showEditWinsDialog("onlineWins", currentUser.getOnlineWins()));
        btnEditBotEasy.setOnClickListener(v -> showEditWinsDialog("botWinsEasy", currentUser.getBotWinsEasy()));
        btnEditBotNormal.setOnClickListener(v -> showEditWinsDialog("botWinsNormal", currentUser.getBotWinsNormal()));
        btnEditBotHard.setOnClickListener(v -> showEditWinsDialog("botWinsHard", currentUser.getBotWinsHard()));
        btnEditOneDeviceWins.setOnClickListener(v -> showEditWinsDialog("oneDeviceWins", currentUser.getOneDeviceWins()));
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
                Log.e(TAG, "Failed to load user", e);
                Toast.makeText(UserProfileActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserData(User user) {
        tvName.setText(user.getUsername() == null ? "-" : user.getUsername());
        tvEmail.setText(user.getEmail() == null ? "-" : user.getEmail());
        tvPassword.setText(user.getPassword() == null ? "-" : user.getPassword());
        tvAdmin.setText((user.isAdmin ? "Yes" : "No"));

        tvOnlineWins.setText(String.valueOf(user.getOnlineWins()));
        tvBotWinsEasy.setText(String.valueOf(user.getBotWinsEasy()));
        tvBotWinsNormal.setText(String.valueOf(user.getBotWinsNormal()));
        tvBotWinsHard.setText(String.valueOf(user.getBotWinsHard()));
        tvOneDeviceWins.setText(String.valueOf(user.getOneDeviceWins()));
    }

    private void showEditWinsDialog(String field, int currentVal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit " + field);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentVal));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String value = input.getText().toString();
            if (!value.isEmpty()) {
                updateWins(field, Integer.parseInt(value));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateWins(String field, int newWins) {
        if (currentUser == null) return;
        
        switch (field) {
            case "onlineWins": currentUser.setOnlineWins(newWins); break;
            case "botWinsEasy": currentUser.setBotWinsEasy(newWins); break;
            case "botWinsNormal": currentUser.setBotWinsNormal(newWins); break;
            case "botWinsHard": currentUser.setBotWinsHard(newWins); break;
            case "oneDeviceWins": currentUser.setOneDeviceWins(newWins); break;
        }

        databaseService.updateUser(currentUser, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(UserProfileActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                showUserData(currentUser);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
