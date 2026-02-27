package com.example.chasergame.screens;

import android.os.Bundle;
import android.widget.Button;

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
import java.util.Collections;
import java.util.List;

public class LeaderBoardActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardEntry> onlineLeaderboard;
    private List<LeaderboardEntry> botLeaderboard;
    private List<LeaderboardEntry> oneDeviceLeaderboard;

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

        Button btnOnline = findViewById(R.id.btn_leaderboard_online);
        Button btnBot = findViewById(R.id.btn_leaderboard_bot);
        Button btnOneDevice = findViewById(R.id.btn_leaderboard_one_device);

        loadLeaderboardData();

        btnOnline.setOnClickListener(v -> {
            if (onlineLeaderboard != null) {
                adapter = new LeaderboardAdapter(onlineLeaderboard);
                recyclerView.setAdapter(adapter);
            }
        });

        btnBot.setOnClickListener(v -> {
            if (botLeaderboard != null) {
                adapter = new LeaderboardAdapter(botLeaderboard);
                recyclerView.setAdapter(adapter);
            }
        });

        btnOneDevice.setOnClickListener(v -> {
            if (oneDeviceLeaderboard != null) {
                adapter = new LeaderboardAdapter(oneDeviceLeaderboard);
                recyclerView.setAdapter(adapter);
            }
        });
    }

    private void loadLeaderboardData() {
        databaseService.getUserList(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                if (users == null) return;

                onlineLeaderboard = new ArrayList<>();
                botLeaderboard = new ArrayList<>();
                oneDeviceLeaderboard = new ArrayList<>();

                for (User user : users) {
                    onlineLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getOnlineWins()));
                    botLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getBotWins()));
                    oneDeviceLeaderboard.add(new LeaderboardEntry(0, user.getUsername(), user.getOneDeviceWins()));
                }

                sortAndRank(onlineLeaderboard);
                sortAndRank(botLeaderboard);
                sortAndRank(oneDeviceLeaderboard);

                // Set the default adapter
                adapter = new LeaderboardAdapter(onlineLeaderboard);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailed(Exception e) {
                // Handle error
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
