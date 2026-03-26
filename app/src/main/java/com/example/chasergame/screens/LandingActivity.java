package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.utils.SharedPreferencesUtil;

public class LandingActivity extends BaseActivity {
    Button BtnReg;
    Button exitBtn;
    Button BtnLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        hideNavigationDrawer();
        hideTopBar();

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

        BtnReg = findViewById(R.id.btn_main_gotosignup);
        if (BtnReg != null) {
            BtnReg.setOnClickListener(view -> {
                Intent intentreg = new Intent(LandingActivity.this, SignupActivity.class);
                startActivity(intentreg);
            });
        }

        BtnLog = findViewById(R.id.btn_main_gotologin);
        if (BtnLog != null) {
            BtnLog.setOnClickListener(view -> {
                Intent intentLog = new Intent(LandingActivity.this, LoginActivity.class);
                startActivity(intentLog);
            });
        }

        exitBtn = findViewById(R.id.btn_main_exit);
        if (exitBtn != null) {
            exitBtn.setOnClickListener(v -> new AlertDialog.Builder(LandingActivity.this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finishAffinity();
                        System.exit(0);
                    })
                    .setNegativeButton("No", null)
                    .show());
        }
    }
}
