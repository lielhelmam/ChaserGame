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

public class OnlineGameActivity extends BaseActivity {

    private String roomId;
    private String playerId;

    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnA, btnB, btnC, btnEndGame;

    private DatabaseReference gameRef;

    private boolean ending = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        roomId = getIntent().getStringExtra("ROOM_ID");
        playerId = getIntent().getStringExtra("PLAYER_ID");

        if (roomId == null || playerId == null) {
            Toast.makeText(this, "Missing game data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        tvTimer = findViewById(R.id.tvTimer);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);

        btnA = findViewById(R.id.btnAnswerA);
        btnB = findViewById(R.id.btnAnswerB);
        btnC = findViewById(R.id.btnAnswerC);
        btnEndGame = findViewById(R.id.btnEndGame);

        gameRef = FirebaseDatabase.getInstance().getReference("games").child(roomId);

        // ❌ לא עושים onDisconnect כאן
        // ❌ לא מוחקים את החדר אוטומטית

        setupGameState();

        btnEndGame.setOnClickListener(v -> endGame());
    }

    private void setupGameState() {

        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(OnlineGameActivity.this, "Game not found", Toast.LENGTH_SHORT).show();
                    goHome();
                    return;
                }

                Long timeMs = snapshot.child("timeMs").getValue(Long.class);
                if (timeMs == null) timeMs = 120_000L;

                tvTurnInfo.setText("Game Started");
                tvTimer.setText(format(timeMs));
                tvScore.setText("P1: 0 | P2: 0");
                tvQuestion.setText("Game logic coming soon...");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(OnlineGameActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void endGame() {
        if (ending) return;
        ending = true;

        gameRef.removeValue().addOnCompleteListener(t -> goHome());
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    private String format(long ms) {
        long sec = ms / 1000;
        long m = sec / 60;
        long s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public void onBackPressed() {
        endGame();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ❌ לא מוחקים חדר כדי לא להרוס משחק
    }
}
