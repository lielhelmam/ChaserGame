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
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.example.chasergame.utils.Validator;

public class EditProfileActivity extends BaseActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnSave, btnCancel;

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

        etUsername = findViewById(R.id.et_edit_username);
        etEmail = findViewById(R.id.et_edit_email);
        etPassword = findViewById(R.id.et_edit_password);
        btnSave = findViewById(R.id.btn_save_profile);
        btnCancel = findViewById(R.id.btn_cancel_profile);

        User current = SharedPreferencesUtil.getUser(this);
        if (current == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etUsername.setText(current.getUsername());
        etEmail.setText(current.getEmail());

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String newName = etUsername.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPass = etPassword.getText().toString().trim();

            if (!Validator.isNameValid(newName)) {
                etUsername.setError("Invalid username");
                etUsername.requestFocus();
                return;
            }

            if (!Validator.isEmailValid(newEmail)) {
                etEmail.setError("Invalid email");
                etEmail.requestFocus();
                return;
            }

            if (!newPass.isEmpty() && !Validator.isPasswordValid(newPass)) {
                etPassword.setError("Password must be at least 6 chars");
                etPassword.requestFocus();
                return;
            }

            boolean emailChanged = current.getEmail() == null || !current.getEmail().equalsIgnoreCase(newEmail);

            if (emailChanged) {
                databaseService.checkIfEmailExists(newEmail, new DatabaseService.DatabaseCallback<Boolean>() {
                    @Override
                    public void onCompleted(Boolean exists) {
                        if (exists) {
                            etEmail.setError("Email already exists");
                            etEmail.requestFocus();
                        } else {
                            saveUser(current, newName, newEmail, newPass);
                        }
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(EditProfileActivity.this, "Check failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                saveUser(current, newName, newEmail, newPass);
            }
        });
    }

    private void saveUser(User current, String newName, String newEmail, String newPass) {
        User updated = new User(
                current.getId(),
                newName,
                newPass.isEmpty() ? current.getPassword() : newPass,
                newEmail,
                current.isAdmin()
        );

        databaseService.updateUser(updated, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                SharedPreferencesUtil.saveUser(EditProfileActivity.this, updated);
                Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
