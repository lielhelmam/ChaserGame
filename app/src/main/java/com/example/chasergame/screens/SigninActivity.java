package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.example.chasergame.utils.Validator;

public class SigninActivity extends BaseActivity {
    private static final String TAG = "SigninActivity";
    private Button goback, btnSubmit;
    private EditText etUserName, etPassword, etEmail, etPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin);

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


        etUserName = findViewById(R.id.EnterUserName);
        etPassword = findViewById(R.id.EnterPassword);
        etEmail = findViewById(R.id.EnterEmail);
        etPhoneNumber = findViewById(R.id.EnterPhoneNumber);

        btnSubmit = findViewById(R.id.btn_signup_submit);
        btnSubmit.setOnClickListener(this::onSubmit);

        goback = findViewById(R.id.btn_signup_goback);
        goback.setOnClickListener(view -> {
            Intent intentreg = new Intent(SigninActivity.this, LandingActivity.class);
            startActivity(intentreg);
        });


    }

    private void onSubmit(View view) {

        Log.d(TAG, "Submit clicked.");

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fName = etUserName.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();

        if (!checkInput(email, password, fName, phone))
            return;

        registerUser(email, password, fName, phone);
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

    private void registerUser(String email, String password, String fName, String phone) {
        Log.d(TAG, "Registering user...");

        String uid = databaseService.generateUserId();
        User user = new User(uid, fName, password, email, false,0,0,0);

        databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean exists) {
                if (exists) {
                    Toast.makeText(SigninActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                } else {
                    createUserInDatabase(user);
                }
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Error checking email", e);
                Toast.makeText(SigninActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInDatabase(User user) {
        databaseService.createNewUser(user, new DatabaseService.DatabaseCallback<Void>() {

            @Override
            public void onCompleted(Void object) {
                Log.d(TAG, "User created successfully");

                SharedPreferencesUtil.saveUser(SigninActivity.this, user);

                Intent mainIntent = new Intent(SigninActivity.this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mainIntent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "Error creating user", e);
                Toast.makeText(SigninActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                SharedPreferencesUtil.signOutUser(SigninActivity.this);
            }
        });
        btnSubmit.setOnClickListener(view -> {
            Intent intenthome = new Intent(SigninActivity.this, MainActivity.class);
            startActivity(intenthome);
        });

    }
}
