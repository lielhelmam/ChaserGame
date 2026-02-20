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
import com.example.chasergame.models.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

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

        initializeDummyData();

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

    private void initializeDummyData() {
        onlineLeaderboard = new ArrayList<>();
        onlineLeaderboard.add(new LeaderboardEntry(1, "Alice", 1000));
        onlineLeaderboard.add(new LeaderboardEntry(2, "Bob", 950));
        onlineLeaderboard.add(new LeaderboardEntry(3, "Charlie", 900));

        botLeaderboard = new ArrayList<>();
        botLeaderboard.add(new LeaderboardEntry(1, "Player1", 500));
        botLeaderboard.add(new LeaderboardEntry(2, "Player2", 450));
        botLeaderboard.add(new LeaderboardEntry(3, "Player3", 400));

        oneDeviceLeaderboard = new ArrayList<>();
        oneDeviceLeaderboard.add(new LeaderboardEntry(1, "John", 1200));
        oneDeviceLeaderboard.add(new LeaderboardEntry(2, "Jane", 1100));
        oneDeviceLeaderboard.add(new LeaderboardEntry(3, "Doe", 1050));
    }
}