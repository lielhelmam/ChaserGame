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

    /**
     * Constructor for RoomService.
     * Initializes the database reference for the 'rooms' node.
     */
    public RoomService() {
        this.roomsRef = FirebaseDatabase.getInstance().getReference("rooms");
    }

    /**
     * Creates a new game room in the database.
     * Generates a unique room code and sets the initial room properties.
     *
     * @param hostId   The ID of the user creating the room.
     * @param hostName The name of the host.
     * @param timeMs   The turn duration in milliseconds.
     * @param isPublic Whether the room is public or private.
     * @param callback Callback to return the result of the room creation.
     */
    public void createRoom(String hostId, String hostName, long timeMs, boolean isPublic, RoomCallback callback) {
        String roomId = generateRoomCode();
        Map<String, Object> room = new HashMap<>();
        room.put("hostId", hostId);
        room.put("hostName", hostName);
        room.put("timeMs", timeMs);
        room.put("status", "waiting");
        room.put("type", isPublic ? "public" : "private");

        Map<String, Object> players = new HashMap<>();
        players.put(hostId, true);
        room.put("players", players);

        roomsRef.child(roomId).setValue(room).addOnCompleteListener(task -> {
            if (task.isSuccessful()) callback.onRoomCreated(roomId);
            else callback.onFailed("Failed to create room");
        });
    }

    /**
     * Searches for an available public room with 'waiting' status and less than 2 players.
     * Joins the first matching room found.
     *
     * @param userId   The ID of the user searching for a room.
     * @param callback Callback to return the result of the search and join attempt.
     */
    public void findAndJoinPublicRoom(String userId, RoomCallback callback) {
        roomsRef.orderByChild("type").equalTo("public").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot roomSnap : snapshot.getChildren()) {
                    String status = roomSnap.child("status").getValue(String.class);
                    long count = roomSnap.child("players").getChildrenCount();
                    if ("waiting".equals(status) && count < 2) {
                        joinRoom(roomSnap.getKey(), userId, callback);
                        return;
                    }
                }
                callback.onFailed("No public rooms available");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.getMessage());
            }
        });
    }

    /**
     * Joins a specific game room by its ID.
     * Checks if the room exists and if it has space for another player.
     *
     * @param roomId   The ID of the room to join.
     * @param userId   The ID of the user joining the room.
     * @param callback Callback to return the result of the join attempt.
     */
    public void joinRoom(String roomId, String userId, RoomCallback callback) {
        roomsRef.child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onFailed("Room not found");
                    return;
                }

                String status = snapshot.child("status").getValue(String.class);
                if (!"waiting".equals(status)) {
                    callback.onFailed("Room is no longer active or is already full");
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
                callback.onJoined(roomId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.getMessage());
            }
        });
    }

    /**
     * Attaches a listener to a specific room to monitor status changes or room closure.
     *
     * @param roomId   The ID of the room to listen to.
     * @param listener The listener to handle status updates.
     */
    public void listenToRoom(String roomId, RoomStatusListener listener) {
        roomsRef.child(roomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onRoomClosed();
                } else {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("cancelled".equals(status) || "finished".equals(status)) {
                        listener.onRoomClosed();
                    } else {
                        listener.onStatusChanged(snapshot);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    /**
     * Updates the room status to 'cancelled' instead of removing it from the database.
     * This keeps the room as a record.
     *
     * @param roomId The ID of the room to be marked as cancelled.
     */
    public void leaveRoom(String roomId) {
        roomsRef.child(roomId).child("status").setValue("cancelled");
    }

    /**
     * Generates a random 6-character alphanumeric room code.
     *
     * @return A unique-ish string for room identification.
     */
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

        void onJoined(String roomId);

        void onFailed(String error);
    }

    public interface RoomStatusListener {
        void onStatusChanged(DataSnapshot snapshot);

        void onRoomClosed();

        void onError(String error);
    }
}
