package com.example.chasergame.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.LeaderboardAdapter;
import com.example.chasergame.models.GameResult;
import com.example.chasergame.models.LeaderboardEntry;
import com.example.chasergame.utils.GameResultsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderBoardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> onlineLeaderboard;
    private List<LeaderboardEntry> botLeaderboard;
    private List<LeaderboardEntry> oneDeviceLeaderboard;

    private Button btnOnline, btnBot, btnOneDevice;

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

        btnOnline = findViewById(R.id.btn_leaderboard_online);
        btnBot = findViewById(R.id.btn_leaderboard_bot);
        btnOneDevice = findViewById(R.id.btn_leaderboard_one_device);

        loadLeaderboardData();

        adapter = new LeaderboardAdapter(onlineLeaderboard);
        recyclerView.setAdapter(adapter);

        btnOnline.setOnClickListener(v -> {
            adapter = new LeaderboardAdapter(onlineLeaderboard);
            recyclerView.setAdapter(adapter);
        });

        btnBot.setOnClickListener(v -> {
            adapter = new LeaderboardAdapter(botLeaderboard);
            recyclerView.setAdapter(adapter);
        });

        btnOneDevice.setOnClickListener(v -> {
            adapter = new LeaderboardAdapter(oneDeviceLeaderboard);
            recyclerView.setAdapter(adapter);
        });
    }

    private void loadLeaderboardData() {
        List<GameResult> gameResults = GameResultsUtil.getGameResults(this);

        Map<String, Integer> onlineWins = new HashMap<>();
        Map<String, Integer> botWins = new HashMap<>();
        Map<String, Integer> oneDeviceWins = new HashMap<>();

        for (GameResult result : gameResults) {
            if (result.isWin()) {
                String username = result.getUsername();
                switch (result.getGameMode()) {
                    case "OnlineGameActivity":
                        onlineWins.put(username, onlineWins.getOrDefault(username, 0) + 1);
                        break;
                    case "PlayAgainstBotActivity":
                        botWins.put(username, botWins.getOrDefault(username, 0) + 1);
                        break;
                    case "PlayOnOneDeviceActivity":
                        oneDeviceWins.put(username, oneDeviceWins.getOrDefault(username, 0) + 1);
                        break;
                }
            }
        }

        onlineLeaderboard = createLeaderboardList(onlineWins);
        botLeaderboard = createLeaderboardList(botWins);
        oneDeviceLeaderboard = createLeaderboardList(oneDeviceWins);
    }

    private List<LeaderboardEntry> createLeaderboardList(Map<String, Integer> winsMap) {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : winsMap.entrySet()) {
            leaderboard.add(new LeaderboardEntry(0, entry.getKey(), entry.getValue()));
        }

        Collections.sort(leaderboard, (e1, e2) -> Integer.compare(e2.getScore(), e1.getScore()));

        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }

        return leaderboard;
    }
}