package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;

public class LandingActivity extends BaseActivity {
    Button BtnReg;
    Button BtnLog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BtnReg = findViewById(R.id.btn_main_gotosignup);
        BtnReg.setOnClickListener(view -> {
            Intent intentreg = new Intent(LandingActivity.this, SigninActivity.class);
            startActivity(intentreg);
        });
        BtnLog = findViewById(R.id.btn_main_gotologin);
        BtnLog.setOnClickListener(view -> {
            Intent intentLog = new Intent(LandingActivity.this, LoginActivity.class);
            startActivity(intentLog);
        });
        }

}