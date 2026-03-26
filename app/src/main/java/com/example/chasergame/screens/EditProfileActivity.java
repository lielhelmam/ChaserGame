package com.example.chasergame.screens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.Validator;

public class EditProfileActivity extends BaseActivity {
    private EditText etUsername, etEmail, etPassword;
    private User user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.EditProfilePage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUI();
    }

    private void initUI() {
        etUsername = findViewById(R.id.et_edit_username);
        etEmail = findViewById(R.id.et_edit_email);
        etPassword = findViewById(R.id.et_edit_password);
        Button btnSave = findViewById(R.id.btn_save_profile);
        Button btnCancel = findViewById(R.id.btn_cancel_profile);

        user = authService.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        etUsername.setText(user.getUsername());
        etEmail.setText(user.getEmail());
        etPassword.setText(user.getPassword());

        btnCancel.setOnClickListener(v -> navigateTo(MainActivity.class, true));
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void validateAndSave() {
        String name = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (!Validator.isNameValid(name)) {
            etUsername.setError("Invalid username");
            return;
        }
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("Invalid email");
            return;
        }
        if (!pass.isEmpty() && !Validator.isPasswordValid(pass)) {
            etPassword.setError("Min 6 chars");
            return;
        }

        boolean emailChanged = user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email);
        if (emailChanged) {
            databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
                @Override
                public void onCompleted(Boolean exists) {
                    if (exists) etEmail.setError("Email exists");
                    else performUpdate(name, email, pass);
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        } else {
            performUpdate(name, email, pass);
        }
    }

    private void performUpdate(String name, String email, String pass) {
        authService.updateProfile(user, name, email, pass, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                navigateTo(MainActivity.class, true);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
