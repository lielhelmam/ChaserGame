package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.Validator;

public class SignupActivity extends BaseActivity {
    private static final String TAG = "SignupActivity";
    private EditText etUserName, etPassword, etEmail, etPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        hideNavigationDrawer();
        hideTopBar();

        if (authService.isUserLoggedIn()) {
            navigateToHome(authService.getCurrentUser());
        }

        etUserName = findViewById(R.id.EnterUserName);
        etPassword = findViewById(R.id.EnterPassword);
        etEmail = findViewById(R.id.EnterEmail);
        etPhoneNumber = findViewById(R.id.EnterPhoneNumber);

        Button btnSubmit = findViewById(R.id.btn_signup_submit);
        if (btnSubmit != null) btnSubmit.setOnClickListener(this::onSubmit);

        findViewById(R.id.btn_signup_goback).setOnClickListener(view ->
                startActivity(new Intent(SignupActivity.this, LandingActivity.class)));
    }

    private void onSubmit(View view) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fName = etUserName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();

        if (checkInput(email, password, fName, phone)) {
            registerUser(email, password, fName);
        }
    }

    private boolean checkInput(String email, String password, String fName, String phone) {
        if (!Validator.isNameValid(fName)) {
            etUserName.setError("Name must be at least 3 chars");
            etUserName.requestFocus();
            return false;
        }
        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("Password must be at least 6 chars");
            etPassword.requestFocus();
            return false;
        }
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("Invalid email");
            etEmail.requestFocus();
            return false;
        }
        if (!Validator.isPhoneValid(phone)) {
            etPhoneNumber.setError("Phone must be 10+ digits");
            etPhoneNumber.requestFocus();
            return false;
        }
        return true;
    }

    private void registerUser(String email, String password, String fName) {
        String uid = databaseService.generateUserId();
        User user = new User(uid, fName, password, email, false, 0, 0, 0);

        authService.register(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                navigateToHome(user);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Registration failed", e);
                Toast.makeText(SignupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome(User user) {
        Class<?> destination = user.isAdmin() ? AdminActivity.class : MainActivity.class;
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
