package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.example.chasergame.services.RoomService;
import com.google.firebase.database.DataSnapshot;

public class WaitingRoomActivity extends BaseActivity {

    private String roomId;
    private String playerId;
    private TextView tvPlayers, tvRoomCode, tvStatus;
    private RoomService roomService;
    private boolean navigatedToGame = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        roomId = getIntent().getStringExtra("ROOM_ID");
        playerId = getIntent().getStringExtra("PLAYER_ID");

        if (roomId == null || playerId == null) {
            Toast.makeText(this, "Missing room data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomService = new RoomService();
        initUI();
        startListening();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leaveRoomAndGoBack();
            }
        });
    }

    private void initUI() {
        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvStatus = findViewById(R.id.tvStatus);
        tvPlayers = findViewById(R.id.tvPlayers);
        Button btnCancel = findViewById(R.id.btnCancelRoom);

        tvRoomCode.setText("Room: " + roomId);
        tvStatus.setText("Waiting for another player...");
        tvPlayers.setText("Players: 1/2");

        btnCancel.setOnClickListener(v -> leaveRoomAndGoBack());
    }

    private void startListening() {
        roomService.listenToRoom(roomId, new RoomService.RoomStatusListener() {
            @Override
            public void onStatusChanged(DataSnapshot snapshot) {
                updateUIFromSnapshot(snapshot);
            }

            @Override
            public void onRoomClosed() {
                if (!navigatedToGame) {
                    Toast.makeText(WaitingRoomActivity.this, "Room closed", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(WaitingRoomActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIFromSnapshot(DataSnapshot snapshot) {
        String type = snapshot.child("type").getValue(String.class);
        tvRoomCode.setVisibility("public".equals(type) ? View.GONE : View.VISIBLE);
        if ("public".equals(type)) tvStatus.setText("Searching for opponent...");

        long playersCount = snapshot.child("players").getChildrenCount();
        tvPlayers.setText("Players: " + playersCount + "/2");

        String status = snapshot.child("status").getValue(String.class);
        if ("ready".equals(status) && playersCount == 2 && !navigatedToGame) {
            navigatedToGame = true;
            goToGame();
        }
    }

    private void goToGame() {
        Intent i = new Intent(this, OnlineGameActivity.class);
        i.putExtra("ROOM_ID", roomId);
        i.putExtra("PLAYER_ID", playerId);
        startActivity(i);
        finish();
    }

    private void leaveRoomAndGoBack() {
        roomService.leaveRoom(roomId);
        finish();
    }
}
