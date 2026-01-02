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

import java.util.HashMap;
import java.util.Map;

public class WaitingRoomActivity extends BaseActivity {

    private String roomId;
    private String playerId;

    private TextView tvRoomCode, tvStatus, tvPlayers;
    private Button btnCancel;

    private DatabaseReference roomRef;
    private ValueEventListener roomListener;

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

        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvStatus = findViewById(R.id.tvStatus);
        tvPlayers = findViewById(R.id.tvPlayers);
        btnCancel = findViewById(R.id.btnCancelRoom);

        tvRoomCode.setText("Room: " + roomId);
        tvStatus.setText("Waiting for another player...");
        tvPlayers.setText("Players: 1/2");

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);

        // ✅ אם האפליקציה נסגרת/נופלת – למחוק חדר אוטומטית (rooms וגם games)
        roomRef.onDisconnect().removeValue();
        FirebaseDatabase.getInstance().getReference("games").child(roomId).onDisconnect().removeValue();

        btnCancel.setOnClickListener(v -> leaveRoomAndGoBack());

        startListening();
    }

    private void startListening() {
        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                // אם החדר לא קיים → משהו מחק אותו
                if (!snapshot.exists()) {
                    Toast.makeText(WaitingRoomActivity.this, "Room closed", Toast.LENGTH_SHORT).show();
                    goBack();
                    return;
                }

                String status = snapshot.child("status").getValue(String.class);
                long playersCount = snapshot.child("players").getChildrenCount();

                tvPlayers.setText("Players: " + playersCount + "/2");

                if ("ready".equals(status)) {
                    // ✅ עוברים למשחק
                    navigatedToGame = true;

                    Intent i = new Intent(WaitingRoomActivity.this, OnlineGameActivity.class);
                    i.putExtra("ROOM_ID", roomId);
                    i.putExtra("PLAYER_ID", playerId);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(WaitingRoomActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        roomRef.addValueEventListener(roomListener);
    }

    private void leaveRoomAndGoBack() {
        // ✅ מוחק גם rooms וגם games בבת אחת
        Map<String, Object> updates = new HashMap<>();
        updates.put("/rooms/" + roomId, null);
        updates.put("/games/" + roomId, null);

        FirebaseDatabase.getInstance().getReference()
                .updateChildren(updates)
                .addOnCompleteListener(task -> goBack());
    }

    private void goBack() {
        finish();
    }

    @Override
    public void onBackPressed() {
        leaveRoomAndGoBack();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomListener != null && roomRef != null) {
            roomRef.removeEventListener(roomListener);
        }

        // ✅ אם יצאנו בלי לעבור למשחק – למחוק (גם אם לא לחצנו cancel)
        if (!navigatedToGame) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/rooms/" + roomId, null);
            updates.put("/games/" + roomId, null);
            FirebaseDatabase.getInstance().getReference().updateChildren(updates);
        }
    }
}
