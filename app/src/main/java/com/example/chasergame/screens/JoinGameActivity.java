package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.RoomService;

public class JoinGameActivity extends BaseActivity {

    private RoomService roomService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);

        roomService = new RoomService();
        EditText etRoomCode = findViewById(R.id.etRoomCode);

        findViewById(R.id.btnJoinRoom).setOnClickListener(v -> {
            String roomId = etRoomCode.getText().toString().trim().toUpperCase();
            if (roomId.isEmpty()) {
                Toast.makeText(this, "Enter room code", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = authService.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            roomService.joinRoom(roomId, user.getId(), new RoomService.RoomCallback() {
                @Override
                public void onRoomCreated(String roomId) {
                }

                @Override
                public void onJoined(String roomId) {
                    Intent i = new Intent(JoinGameActivity.this, WaitingRoomActivity.class);
                    i.putExtra("ROOM_ID", roomId);
                    i.putExtra("PLAYER_ID", user.getId());
                    startActivity(i);
                    finish();
                }

                @Override
                public void onFailed(String error) {
                    Toast.makeText(JoinGameActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
