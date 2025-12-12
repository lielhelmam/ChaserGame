package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.example.chasergame.utils.Validator;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";
    Button goback;
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.MainPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        User user = SharedPreferencesUtil.getUser(this);

        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            Intent intent;

            if (user != null && user.isAdmin()) {
                intent = new Intent(this, AdminActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }


        etUsername = findViewById(R.id.LoginEnterUserName);
        etPassword = findViewById(R.id.Login_EnterPassword);
        btnLogin = findViewById(R.id.btn_login_check);
        tvRegister = findViewById(R.id.Login_tv_register);

        btnLogin.setOnClickListener(this);
        tvRegister.setOnClickListener(this);

        goback = findViewById(R.id.btn_login_goback);
        goback.setOnClickListener(view -> {
            Intent intentreg = new Intent(LoginActivity.this, LandingActivity.class);
            startActivity(intentreg);
        });


    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnLogin.getId()) {

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!checkInput(username, password)) return;

            loginUser(username, password);

        } else if (v.getId() == tvRegister.getId()) {
            startActivity(new Intent(LoginActivity.this, SigninActivity.class));
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
        databaseService.getUserByUsernameAndPassword(username, password, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {

                if (user == null) { // ✅ THIS is the real "invalid credentials" case
                    etPassword.setError("Invalid username or password");
                    etPassword.requestFocus();
                    SharedPreferencesUtil.signOutUser(LoginActivity.this);
                    return;
                }

                SharedPreferencesUtil.saveUser(LoginActivity.this, user);

                Intent intent = user.isAdmin()
                        ? new Intent(LoginActivity.this, AdminActivity.class)
                        : new Intent(LoginActivity.this, MainActivity.class);

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onFailed(Exception e) {
                etPassword.setError("Login failed: " + e.getMessage());
                etPassword.requestFocus();
                SharedPreferencesUtil.signOutUser(LoginActivity.this);
            }
        });
    }
    }

