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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OnlineGameActivity extends BaseActivity {

    private String roomId;
    private String playerId;

    private TextView tvTurnInfo;
    private Button btnEndGame;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        roomId = getIntent().getStringExtra("ROOM_ID");
        playerId = getIntent().getStringExtra("PLAYER_ID");

        if (roomId == null || playerId == null) {
            Toast.makeText(this, "Missing room data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        btnEndGame = findViewById(R.id.btnEndGame);

        tvTurnInfo.setText("Online game started!\nRoom: " + roomId);

        // read timeMs just to confirm it exists
        FirebaseDatabase.getInstance()
                .getReference("rooms")
                .child(roomId)
                .child("timeMs")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Long timeMs = snapshot.getValue(Long.class);
                        if (timeMs == null) timeMs = 120_000L;
                        tvTurnInfo.setText("Online game started!\nRoom: " + roomId + "\nTurn time: " + (timeMs / 1000) + " sec");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(OnlineGameActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        btnEndGame.setOnClickListener(v -> endGameAndDeleteRoom());
    }

    private void endGameAndDeleteRoom() {
        // delete room always (both players can call it, doesn't matter)
        FirebaseDatabase.getInstance()
                .getReference("rooms")
                .child(roomId)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Room deleted", Toast.LENGTH_SHORT).show();
                    goHome();
                });
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
