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

    private TextView tvName, tvEmail, tvPassword, tvAdmin, tvOnlineWins, tvBotWins, tvOneDeviceWins;
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
        tvBotWins = findViewById(R.id.tv_bot_wins);
        tvOneDeviceWins = findViewById(R.id.tv_one_device_wins);
        Button btnEditOnlineWins = findViewById(R.id.btn_edit_online_wins);
        Button btnEditBotWins = findViewById(R.id.btn_edit_bot_wins);
        Button btnEditOneDeviceWins = findViewById(R.id.btn_edit_one_device_wins);


        userId = getIntent().getStringExtra("USER_UID");
        if (userId == null || userId.trim().isEmpty()) {
            Log.e(TAG, "No USER_UID passed to activity");
            Toast.makeText(this, "No user specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUser();

        btnEditOnlineWins.setOnClickListener(v -> showEditWinsDialog("onlineWins"));
        btnEditBotWins.setOnClickListener(v -> showEditWinsDialog("botWins"));
        btnEditOneDeviceWins.setOnClickListener(v -> showEditWinsDialog("oneDeviceWins"));
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
        tvBotWins.setText(String.valueOf(user.getBotWins()));
        tvOneDeviceWins.setText(String.valueOf(user.getOneDeviceWins()));
    }

    private void showEditWinsDialog(String field) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Wins");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText().toString();
            if (value.isEmpty()) {
                return;
            }
            int newWins = Integer.parseInt(value);
            updateWins(field, newWins);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateWins(String field, int newWins) {
        if (currentUser == null) {
            return;
        }
        switch (field) {
            case "onlineWins":
                currentUser.setOnlineWins(newWins);
                break;
            case "botWins":
                currentUser.setBotWins(newWins);
                break;
            case "oneDeviceWins":
                currentUser.setOneDeviceWins(newWins);
                break;
        }

        databaseService.updateUser(currentUser, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(UserProfileActivity.this, "Wins updated", Toast.LENGTH_SHORT).show();
                showUserData(currentUser);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(UserProfileActivity.this, "Failed to update wins", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
