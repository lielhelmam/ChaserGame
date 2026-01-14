package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class JoinGameActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        EditText etRoomCode = findViewById(R.id.etRoomCode);

        findViewById(R.id.btnJoinRoom).setOnClickListener(v -> {

            String roomId = etRoomCode.getText().toString().trim().toUpperCase();

            if (roomId.isEmpty()) {
                Toast.makeText(this, "Enter room code", Toast.LENGTH_SHORT).show();
                return;
            }

            String playerId = UUID.randomUUID().toString();

            FirebaseDatabase.getInstance()
                    .getReference("rooms")
                    .child(roomId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {

                            if (!snapshot.exists()) {
                                Toast.makeText(JoinGameActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            long players = snapshot.child("players").getChildrenCount();

                            if (players >= 2) {
                                Toast.makeText(JoinGameActivity.this, "Room full", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // הוספת שחקן
                            snapshot.getRef()
                                    .child("players")
                                    .child(playerId)
                                    .setValue(true);

                            // אם זה שחקן 2 → הפעל משחק
                            if (players == 1) {
                                snapshot.getRef().child("status").setValue("ready");
                            }

                            // מעבר לחדר המתנה
                            Intent i = new Intent(JoinGameActivity.this, WaitingRoomActivity.class);
                            i.putExtra("ROOM_ID", roomId);
                            i.putExtra("PLAYER_ID", playerId);
                            startActivity(i);
                            finish();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(JoinGameActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
