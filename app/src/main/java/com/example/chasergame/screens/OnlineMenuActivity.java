package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;

import com.example.chasergame.R;

public class OnlineMenuActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_menu);
        setTopBarTitle("Online Game");

        findViewById(R.id.btnCreateGame).setOnClickListener(v ->
                navigateTo(ChooseTimeOnlineActivity.class, false));

        findViewById(R.id.btnJoinGame).setOnClickListener(v ->
                navigateTo(JoinGameActivity.class, false));

        // For now, handling public the same as private since we don't have a separate logic yet
        findViewById(R.id.btnCreatePublicGame).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChooseTimeOnlineActivity.class);
            intent.putExtra("IS_PUBLIC", true);
            startActivity(intent);
        });

        findViewById(R.id.btnJoinPublicGame).setOnClickListener(v -> joinPublicGame());
    }

    private void joinPublicGame() {
        String userId = authService.getCurrentUser().getId();
        new com.example.chasergame.services.RoomService().findAndJoinPublicRoom(userId, new com.example.chasergame.services.RoomService.RoomCallback() {
            @Override
            public void onRoomCreated(String roomId) {
            }

            @Override
            public void onJoined(String roomId) {
                Intent i = new Intent(OnlineMenuActivity.this, WaitingRoomActivity.class);
                i.putExtra("ROOM_ID", roomId);
                i.putExtra("PLAYER_ID", userId);
                startActivity(i);
            }

            @Override
            public void onFailed(String error) {
                android.widget.Toast.makeText(OnlineMenuActivity.this, error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
}
