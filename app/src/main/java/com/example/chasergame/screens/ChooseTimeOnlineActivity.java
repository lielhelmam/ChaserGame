package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.RoomService;

public class ChooseTimeOnlineActivity extends BaseActivity {

    private RoomService roomService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_time_online);

        roomService = new RoomService();
        boolean isPublic = getIntent().getBooleanExtra("IS_PUBLIC", false);

        NumberPicker pickerMinutes = findViewById(R.id.pickerMinutes);
        NumberPicker pickerSeconds = findViewById(R.id.pickerSeconds);

        pickerMinutes.setMinValue(0);
        pickerMinutes.setMaxValue(10);
        pickerSeconds.setMinValue(0);
        pickerSeconds.setMaxValue(59);

        findViewById(R.id.btnCreateRoom).setOnClickListener(v -> {
            int minutes = pickerMinutes.getValue();
            int seconds = pickerSeconds.getValue();

            if (minutes == 0 && seconds == 0) {
                Toast.makeText(this, "Choose valid time", Toast.LENGTH_SHORT).show();
                return;
            }

            long timeMs = (minutes * 60L + seconds) * 1000L;
            User user = authService.getCurrentUser();

            if (user == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            roomService.createRoom(user.getId(), user.getUsername(), timeMs, new RoomService.RoomCallback() {
                @Override
                public void onRoomCreated(String roomId) {
                    Intent i = new Intent(ChooseTimeOnlineActivity.this, WaitingRoomActivity.class);
                    i.putExtra("ROOM_ID", roomId);
                    i.putExtra("PLAYER_ID", user.getId());
                    startActivity(i);
                    finish();
                }

                @Override
                public void onJoined() {
                }

                @Override
                public void onFailed(String error) {
                    Toast.makeText(ChooseTimeOnlineActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
