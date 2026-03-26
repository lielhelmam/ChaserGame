package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.chasergame.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class OnlineMenuActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_menu);
        setTopBarTitle("Online Trivia");

        // Private options
        findViewById(R.id.btnCreateGame).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChooseTimeOnlineActivity.class);
            intent.putExtra("IS_PUBLIC", false);
            startActivity(intent);
        });

        findViewById(R.id.btnJoinGame).setOnClickListener(v ->
                startActivity(new Intent(this, JoinGameActivity.class)));

        // Public options
        findViewById(R.id.btnCreatePublicGame).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChooseTimeOnlineActivity.class);
            intent.putExtra("IS_PUBLIC", true);
            startActivity(intent);
        });

        findViewById(R.id.btnJoinPublicGame).setOnClickListener(v -> findPublicGame());
    }

    private void findPublicGame() {
        Toast.makeText(this, "Searching for a match...", Toast.LENGTH_SHORT).show();

        FirebaseDatabase.getInstance().getReference("rooms")
                .orderByChild("type").equalTo("public")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot roomSnap : snapshot.getChildren()) {
                            String status = roomSnap.child("status").getValue(String.class);
                            long playerCount = roomSnap.child("players").getChildrenCount();

                            if ("waiting".equals(status) && playerCount == 1) {
                                String roomId = roomSnap.getKey();
                                String playerId = UUID.randomUUID().toString();

                                roomSnap.child("players").child(playerId).getRef().setValue(true)
                                        .addOnSuccessListener(unused -> {
                                            Intent intent = new Intent(OnlineMenuActivity.this, WaitingRoomActivity.class);
                                            intent.putExtra("ROOM_ID", roomId);
                                            intent.putExtra("PLAYER_ID", playerId);
                                            startActivity(intent);
                                            finish();
                                        });
                                return;
                            }
                        }
                        Toast.makeText(OnlineMenuActivity.this, "No public games found. Create one!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(OnlineMenuActivity.this, "Error searching for game", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
