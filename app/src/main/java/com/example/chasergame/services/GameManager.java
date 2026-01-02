package com.example.chasergame.services;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GameManager {

    private static final DatabaseReference gamesRef =
            FirebaseDatabase.getInstance().getReference("games");

    public static DatabaseReference createGame(String userId) {
        DatabaseReference gameRef = gamesRef.push();

        gameRef.child("status").setValue("waiting");
        gameRef.child("createdAt").setValue(System.currentTimeMillis());
        gameRef.child("players").child("p1").child("uid").setValue(userId);

        return gameRef;
    }

    public static void joinGame(String gameId, String userId) {
        DatabaseReference gameRef = gamesRef.child(gameId);

        gameRef.child("players").child("p2").child("uid").setValue(userId);
        gameRef.child("status").setValue("playing");
    }

    public static void deleteGame(String gameId) {
        gamesRef.child(gameId).removeValue();
    }
}
