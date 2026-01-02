package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ChooseTimeOnlineActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_time_online);

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

            String roomId = generateRoomCode();
            String playerId = UUID.randomUUID().toString();

            Map<String, Object> room = new HashMap<>();
            room.put("status", "waiting");
            room.put("timeMs", timeMs);
            room.put("hostId", playerId);

            Map<String, Object> players = new HashMap<>();
            players.put(playerId, true);
            room.put("players", players);

            FirebaseDatabase.getInstance()
                    .getReference("rooms")
                    .child(roomId)
                    .setValue(room)
                    .addOnSuccessListener(unused -> {
                        Intent i = new Intent(this, WaitingRoomActivity.class);
                        i.putExtra("ROOM_ID", roomId);
                        i.putExtra("PLAYER_ID", playerId);
                        startActivity(i);
                        finish();
                    });
        });
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
