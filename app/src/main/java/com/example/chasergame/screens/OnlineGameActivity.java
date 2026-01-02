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

public class OnlineGameActivity extends BaseActivity {

    private String roomId;
    private String playerId;

    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnAnswerA, btnAnswerB, btnAnswerC, btnEndGame;

    private DatabaseReference roomRef;
    private boolean finishing = false;

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
        tvTimer = findViewById(R.id.tvTimer);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);

        btnAnswerA = findViewById(R.id.btnAnswerA);
        btnAnswerB = findViewById(R.id.btnAnswerB);
        btnAnswerC = findViewById(R.id.btnAnswerC);
        btnEndGame = findViewById(R.id.btnEndGame);

        // ✅ אצלך זה /games
        roomRef = FirebaseDatabase.getInstance().getReference("games").child(roomId);

        // ✅ אם האפליקציה תתנתק/תקרוס — למחוק את החדר
        roomRef.onDisconnect().removeValue();

        // רק כדי להראות שהחדר קיים + timeMs (אופציונלי)
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(OnlineGameActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                    goHome();
                    return;
                }

                Long timeMs = snapshot.child("timeMs").getValue(Long.class);
                if (timeMs == null) timeMs = 120_000L;

                tvTurnInfo.setText("Online Game");
                tvTimer.setText(formatTime(timeMs));
                tvScore.setText("P1: 0 | P2: 0");
                tvQuestion.setText("Waiting for game logic...");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OnlineGameActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnEndGame.setOnClickListener(v -> endGameAndDeleteRoom());
    }

    private void endGameAndDeleteRoom() {
        if (finishing) return;
        finishing = true;

        roomRef.removeValue().addOnCompleteListener(task -> {
            Toast.makeText(this, "Game ended. Room deleted.", Toast.LENGTH_SHORT).show();
            goHome();
        });
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        endGameAndDeleteRoom();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // ✅ אם יצאת מהמשחק (home / recent apps / סגרת) — למחוק חדר
        if (!finishing) {
            finishing = true;
            roomRef.removeValue();
        }
    }

    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
