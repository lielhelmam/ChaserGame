package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    private TextView tvPlayers;

    private DatabaseReference roomRef;
    private ValueEventListener roomListener;

    private boolean navigatedToGame = false;
    private boolean destroyed = false;

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

        TextView tvRoomCode = findViewById(R.id.tvRoomCode);
        TextView tvStatus = findViewById(R.id.tvStatus);
        tvPlayers = findViewById(R.id.tvPlayers);
        Button btnCancel = findViewById(R.id.btnCancelRoom);

        tvRoomCode.setText("Room: " + roomId);
        tvStatus.setText("Waiting for another player...");
        tvPlayers.setText("Players: 1/2");

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);

        btnCancel.setOnClickListener(v -> leaveRoomAndGoBack());

        startListening();
    }

    private void startListening() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    if (!destroyed) {
                        Toast.makeText(WaitingRoomActivity.this, "Room closed", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    return;
                }

                long playersCount = snapshot.child("players").getChildrenCount();
                tvPlayers.setText("Players: " + playersCount + "/2");

                String status = snapshot.child("status").getValue(String.class);

                if ("ready".equals(status) && playersCount == 2 && !navigatedToGame) {
                    navigatedToGame = true;
                    goToGame();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WaitingRoomActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        roomRef.addValueEventListener(roomListener);
    }

    private void goToGame() {
        Intent i = new Intent(WaitingRoomActivity.this, OnlineGameActivity.class);
        i.putExtra("ROOM_ID", roomId);
        i.putExtra("PLAYER_ID", playerId);
        startActivity(i);
        finish();
    }

    private void leaveRoomAndGoBack() {
        roomRef.removeValue()
                .addOnCompleteListener(task -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;

        if (roomListener != null && roomRef != null) {
            roomRef.removeEventListener(roomListener);
        }

        // אם לא התקדמנו למשחק — מחיקה
        if (!navigatedToGame) {
            roomRef.removeValue();
        }
    }

    @Override
    public void onBackPressed() {
        leaveRoomAndGoBack();
    }
}
