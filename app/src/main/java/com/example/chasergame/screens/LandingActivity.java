package com.example.chasergame.screens;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.LandingPage), (v, insets) -> {
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

        exitBtn = findViewById(R.id.btn_main_exit);
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(LandingActivity.this)
                        .setTitle("Exit App")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAffinity();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        /*
        DatabaseService.getInstance().getQuestionList(new DatabaseService.DatabaseCallback<List<Question>>() {
            @Override
            public void onCompleted(List<Question> questions) {
                Log.d("!!!!!!!!!!!!!!!!!", questions.size()+"");
                for (Question question : questions) {
                    Log.d("????????????", question.toString());
                }
            }

            @Override
            public void onFailed(Exception e) {

            }
        });
         */
    }
}