package com.example.chasergame.screens;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.LeaderboardAdapter;
import com.example.chasergame.models.LeaderboardEntry;
import com.example.chasergame.services.LeaderboardService;

import java.util.List;

public class LeaderBoardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private List<LeaderboardEntry> rhythmList, onlineList, botEasyList, botNormalList, botHardList;

    private LinearLayout botFilters;
    private Button btnEasy, btnNormal, btnHard;
    private TextView tvHeaderStat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leader_board);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        botFilters = findViewById(R.id.bot_difficulty_filters);
        btnEasy = findViewById(R.id.btn_bot_easy);
        btnNormal = findViewById(R.id.btn_bot_normal);
        btnHard = findViewById(R.id.btn_bot_hard);
        tvHeaderStat = findViewById(R.id.tv_header_stat);

        setupMainFilters();
        setupDifficultyFilters();
    }

    private void setupMainFilters() {
        Button btnRhythm = findViewById(R.id.btn_leaderboard_rhythm);
        Button btnOnline = findViewById(R.id.btn_leaderboard_online);
        Button btnBot = findViewById(R.id.btn_leaderboard_bot);

        btnRhythm.setOnClickListener(v -> {
            botFilters.setVisibility(View.GONE);
            tvHeaderStat.setText("Score");
            updateButtonColors(btnRhythm, btnOnline, btnBot);
            showLeaderboard(rhythmList);
        });

        btnOnline.setOnClickListener(v -> {
            botFilters.setVisibility(View.GONE);
            tvHeaderStat.setText("Wins");
            updateButtonColors(btnOnline, btnRhythm, btnBot);
            showLeaderboard(onlineList);
        });

        btnBot.setOnClickListener(v -> {
            botFilters.setVisibility(View.VISIBLE);
            tvHeaderStat.setText("Wins");
            updateButtonColors(btnBot, btnRhythm, btnOnline);
            updateDifficultyColors(btnNormal, btnEasy, btnHard);
            showLeaderboard(botNormalList);
        });
    }

    private void setupDifficultyFilters() {
        btnEasy.setOnClickListener(v -> {
            updateDifficultyColors(btnEasy, btnNormal, btnHard);
            showLeaderboard(botEasyList);
        });
        btnNormal.setOnClickListener(v -> {
            updateDifficultyColors(btnNormal, btnEasy, btnHard);
            showLeaderboard(botNormalList);
        });
        btnHard.setOnClickListener(v -> {
            updateDifficultyColors(btnHard, btnEasy, btnNormal);
            showLeaderboard(botHardList);
        });
    }

    private void loadData() {
        leaderboardService.loadAllLeaderboards(new LeaderboardService.LeaderboardCallback() {
            @Override
            public void onDataReady(List<LeaderboardEntry> rhythm, List<LeaderboardEntry> online,
                                    List<LeaderboardEntry> botEasy, List<LeaderboardEntry> botNormal,
                                    List<LeaderboardEntry> botHard) {
                rhythmList = rhythm;
                onlineList = online;
                botEasyList = botEasy;
                botNormalList = botNormal;
                botHardList = botHard;

                showLeaderboard(rhythmList); // Default
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(LeaderBoardActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLeaderboard(List<LeaderboardEntry> list) {
        if (list != null) {
            recyclerView.setAdapter(new LeaderboardAdapter(list));
        }
    }

    private void updateButtonColors(Button active, Button... others) {
        active.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.background_red)));
        active.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        for (Button b : others) {
            b.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
            b.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.background_red)));
        }
    }

    private void updateDifficultyColors(Button active, Button... others) {
        active.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.background_red)));
        active.setTextColor(0xFFFFFFFF);
        for (Button b : others) {
            b.setBackgroundTintList(ColorStateList.valueOf(0x00000000));
            b.setTextColor(0xFF333333);
        }
    }
}
