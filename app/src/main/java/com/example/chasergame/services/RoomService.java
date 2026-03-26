package com.example.chasergame.services;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RoomService {
    private final DatabaseReference roomsRef;

    public RoomService() {
        this.roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
    }

    public void createRoom(String hostId, String hostName, long timeMs, RoomCallback callback) {
        String roomId = generateRoomCode();
        Map<String, Object> room = new HashMap<>();
        room.put("hostId", hostId);
        room.put("hostName", hostName);
        room.put("timeMs", timeMs);
        room.put("status", "waiting");

        Map<String, Object> players = new HashMap<>();
        players.put(hostId, true);
        room.put("players", players);

        roomsRef.child(roomId).setValue(room).addOnCompleteListener(task -> {
            if (task.isSuccessful()) callback.onRoomCreated(roomId);
            else callback.onFailed("Failed to create room");
        });
    }

    public void joinRoom(String roomId, String userId, RoomCallback callback) {
        roomsRef.child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onFailed("Room not found");
                    return;
                }

                long count = snapshot.child("players").getChildrenCount();
                if (count >= 2) {
                    callback.onFailed("Room full");
                    return;
                }

                snapshot.getRef().child("players").child(userId).setValue(true);
                if (count == 1) {
                    snapshot.getRef().child("status").setValue("ready");
                }
                callback.onJoined();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.getMessage());
            }
        });
    }

    public void listenToRoom(String roomId, RoomStatusListener listener) {
        roomsRef.child(roomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onRoomClosed();
                } else {
                    listener.onStatusChanged(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public void leaveRoom(String roomId) {
        roomsRef.child(roomId).removeValue();
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.util.Random r = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public interface RoomCallback {
        void onRoomCreated(String roomId);

        void onJoined();

        void onFailed(String error);
    }

    public interface RoomStatusListener {
        void onStatusChanged(DataSnapshot snapshot);

        void onRoomClosed();

        void onError(String error);
    }
}
