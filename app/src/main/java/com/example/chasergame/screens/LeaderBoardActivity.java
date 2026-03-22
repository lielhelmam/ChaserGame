package com.example.chasergame.screens;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.LeaderboardAdapter;
import com.example.chasergame.models.LeaderboardEntry;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class LeaderBoardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> onlineLeaderboard;
    private List<LeaderboardEntry> botEasyLeaderboard;
    private List<LeaderboardEntry> botNormalLeaderboard;
    private List<LeaderboardEntry> botHardLeaderboard;
    private List<LeaderboardEntry> oneDeviceLeaderboard;
    private List<LeaderboardEntry> rhythmLeaderboard;

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

        recyclerView = findViewById(R.id.rv_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        botFilters = findViewById(R.id.bot_difficulty_filters);
        btnEasy = findViewById(R.id.btn_bot_easy);
        btnNormal = findViewById(R.id.btn_bot_normal);
        btnHard = findViewById(R.id.btn_bot_hard);
        tvHeaderStat = findViewById(R.id.tv_header_stat);

        Button btnRhythm = findViewById(R.id.btn_leaderboard_rhythm);
        Button btnOnline = findViewById(R.id.btn_leaderboard_online);
        Button btnBot = findViewById(R.id.btn_leaderboard_bot);
        Button btnOneDevice = findViewById(R.id.btn_leaderboard_one_device);

        loadLeaderboardData();

        btnRhythm.setOnClickListener(v -> {
            botFilters.setVisibility(View.GONE);
            tvHeaderStat.setText("Score");
            updateButtonColors(btnRhythm, btnOnline, btnBot, btnOneDevice);
            showLeaderboard(rhythmLeaderboard);
        });

        btnOnline.setOnClickListener(v -> {
            botFilters.setVisibility(View.GONE);
            tvHeaderStat.setText("Wins");
            updateButtonColors(btnOnline, btnRhythm, btnBot, btnOneDevice);
            showLeaderboard(onlineLeaderboard);
        });

        btnBot.setOnClickListener(v -> {
            botFilters.setVisibility(View.VISIBLE);
            tvHeaderStat.setText("Wins");
            updateButtonColors(btnBot, btnRhythm, btnOnline, btnOneDevice);
            updateDifficultyColors(btnNormal, btnEasy, btnHard);
            showLeaderboard(botNormalLeaderboard);
        });

        btnOneDevice.setOnClickListener(v -> {
            botFilters.setVisibility(View.GONE);
            tvHeaderStat.setText("Wins");
            updateButtonColors(btnOneDevice, btnRhythm, btnOnline, btnBot);
            showLeaderboard(oneDeviceLeaderboard);
        });

        btnEasy.setOnClickListener(v -> {
            updateDifficultyColors(btnEasy, btnNormal, btnHard);
            showLeaderboard(botEasyLeaderboard);
        });
        btnNormal.setOnClickListener(v -> {
            updateDifficultyColors(btnNormal, btnEasy, btnHard);
            showLeaderboard(botNormalLeaderboard);
        });
        btnHard.setOnClickListener(v -> {
            updateDifficultyColors(btnHard, btnEasy, btnNormal);
            showLeaderboard(botHardLeaderboard);
        });
    }

    private void showLeaderboard(List<LeaderboardEntry> list) {
        if (list != null) {
            adapter = new LeaderboardAdapter(list);
            recyclerView.setAdapter(adapter);
        }
    }

    private void updateButtonColors(Button active, Button... others) {
        active.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.background_red)));
        for (Button b : others) {
            b.setBackgroundTintList(ColorStateList.valueOf(0xFF333333));
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

    private void loadLeaderboardData() {
        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users == null) return;

                rhythmLeaderboard = new ArrayList<>();
                onlineLeaderboard = new ArrayList<>();
                botEasyLeaderboard = new ArrayList<>();
                botNormalLeaderboard = new ArrayList<>();
                botHardLeaderboard = new ArrayList<>();
                oneDeviceLeaderboard = new ArrayList<>();

                for (User user : users) {
                    if (user.getTotalRhythmScore() > 0) {
                        rhythmLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getTotalRhythmScore()));
                    }
                    if (user.getOnlineWins() > 0) {
                        onlineLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getOnlineWins()));
                    }
                    if (user.getBotWinsEasy() > 0) {
                        botEasyLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getBotWinsEasy()));
                    }
                    if (user.getBotWinsNormal() > 0) {
                        botNormalLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getBotWinsNormal()));
                    }
                    if (user.getBotWinsHard() > 0) {
                        botHardLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getBotWinsHard()));
                    }
                    if (user.getOneDeviceWins() > 0) {
                        oneDeviceLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getOneDeviceWins()));
                    }
                }

                sortAndRank(rhythmLeaderboard);
                sortAndRank(onlineLeaderboard);
                sortAndRank(botEasyLeaderboard);
                sortAndRank(botNormalLeaderboard);
                sortAndRank(botHardLeaderboard);
                sortAndRank(oneDeviceLeaderboard);

                tvHeaderStat.setText("Score"); // Default to Rhythm
                showLeaderboard(rhythmLeaderboard);
            }

            @Override
            public void onFailed(Exception e) {
            }
        });
    }

    private void sortAndRank(List<LeaderboardEntry> leaderboard) {
        leaderboard.sort((e1, e2) -> Integer.compare(e2.getScore(), e1.getScore()));
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }
    }
}
