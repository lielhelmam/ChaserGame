package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaitingRoomActivity extends BaseActivity {

    private String roomId;
    private String playerId;

    private TextView tvRoomCode, tvStatus, tvPlayers;
    private Button btnCancel;

    private DatabaseReference roomRef;
    private boolean navigated = false;

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

        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvStatus = findViewById(R.id.tvStatus);
        tvPlayers = findViewById(R.id.tvPlayers);
        btnCancel = findViewById(R.id.btnCancelRoom);

        tvRoomCode.setText("Room: " + roomId);
        tvStatus.setText("Waiting for another player...");
        tvPlayers.setText("Players: 1 / 2");

        roomRef = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .child(roomId);

        btnCancel.setOnClickListener(v -> leaveRoom());

        startListening();
    }

    private void startListening() {
        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvPlayers.setText("Players: " + count + " / 2");

                if (count >= 2 && !navigated) {
                    navigated = true;
                    goToGame();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(WaitingRoomActivity.this,
                        "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToGame() {
        roomRef.child("status").setValue("playing");

        Intent i = new Intent(this, OnlineGameActivity.class);
        i.putExtra("ROOM_ID", roomId);
        i.putExtra("PLAYER_ID", playerId);
        startActivity(i);
        finish();
    }

    private void leaveRoom() {
        roomRef.removeValue();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!navigated) {
            roomRef.removeValue();
        }
    }
}
