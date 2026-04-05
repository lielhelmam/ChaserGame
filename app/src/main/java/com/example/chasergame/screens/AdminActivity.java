package com.example.chasergame.screens;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;

public class AdminActivity extends BaseActivity {

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

        setupButtons();
    }

    private void setupButtons() {
        findViewById(R.id.btnAddQuestion).setOnClickListener(v ->
                navigateTo(AddQuestionActivity.class, false));

        findViewById(R.id.btn_admin_questions_list).setOnClickListener(v ->
                navigateTo(QuestionsListActivity.class, false));

        findViewById(R.id.btn_admin_gotouserlist).setOnClickListener(v ->
                navigateTo(UsersListActivity.class, false));

        findViewById(R.id.btn_admin_manage_songs).setOnClickListener(v ->
                navigateTo(ManageSongsActivity.class, false));

        findViewById(R.id.btn_admin_songs_list).setOnClickListener(v ->
                navigateTo(SongsListActivity.class, false));

    }
}
