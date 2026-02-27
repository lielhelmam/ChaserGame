package com.example.chasergame.screens;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.utils.SharedPreferencesUtil;

public class AdminActivity extends BaseActivity {
    Button Logout;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.AdminPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Button btnAddQuestion = findViewById(R.id.btnAddQuestion);

        btnAddQuestion.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AddQuestionActivity.class);
            startActivity(intent);
        });

        Button btnQuestionsList = findViewById(R.id.btn_admin_questions_list);

        btnQuestionsList.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, QuestionsListActivity.class);
            startActivity(intent);
        });

        btn = findViewById(R.id.btn_admin_gotouserlist);
        Log.d(TAG, "Navigating to UsersListActivity");

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, UsersListActivity.class);
            startActivity(intent);
        });


        Logout = findViewById(R.id.btn_admin_logout);
        Logout.setOnClickListener(v -> signOut());


    }

    private void signOut() {
        Log.d(TAG, "Sign out button clicked");
        SharedPreferencesUtil.signOutUser(AdminActivity.this);

        Log.d(TAG, "User signed out, redirecting to LandingActivity");
        Intent landingIntent = new Intent(AdminActivity.this, LandingActivity.class);
        landingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(landingIntent);
    }

}