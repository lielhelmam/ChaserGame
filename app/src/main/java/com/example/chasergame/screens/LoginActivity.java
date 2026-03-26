package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.Validator;

public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        hideNavigationDrawer();
        hideTopBar();

        if (authService.isUserLoggedIn()) {
            navigateToHome(authService.getCurrentUser());
        }

        etUsername = findViewById(R.id.LoginEnterUserName);
        etPassword = findViewById(R.id.Login_EnterPassword);
        btnLogin = findViewById(R.id.btn_login_check);
        tvRegister = findViewById(R.id.Login_tv_register);

        if (btnLogin != null) btnLogin.setOnClickListener(this);
        if (tvRegister != null) tvRegister.setOnClickListener(this);

        findViewById(R.id.btn_login_goback).setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, LandingActivity.class)));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_login_check) {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (checkInput(username, password)) {
                loginUser(username, password);
            }
        } else if (v.getId() == R.id.Login_tv_register) {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        }
    }

    private boolean checkInput(String username, String password) {
        if (!Validator.isNameValid(username)) {
            etUsername.setError("Invalid username");
            etUsername.requestFocus();
            return false;
        }
        if (!Validator.isPasswordValid(password)) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void loginUser(String username, String password) {
        authService.login(username, password, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user == null) {
                    etPassword.setError("Invalid username or password");
                    etPassword.requestFocus();
                    authService.logout();
                } else {
                    navigateToHome(user);
                }
            }

            @Override
            public void onFailed(Exception e) {
                etPassword.setError("Login failed: " + e.getMessage());
                etPassword.requestFocus();
                authService.logout();
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
