package com.example.chasergame.services;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GameManager {

    private static final DatabaseReference gamesRef =
            FirebaseDatabase.getInstance().getReference("games");

    /**
     * Creates a new game instance in the database.
     * Initializes the game status to 'waiting' and sets the creator as player 1.
     * @param userId The ID of the user creating the game.
     * @return A DatabaseReference pointing to the newly created game.
     */
    public static DatabaseReference createGame(String userId) {
        DatabaseReference gameRef = gamesRef.push();

        gameRef.child("status").setValue("waiting");
        gameRef.child("createdAt").setValue(System.currentTimeMillis());
        gameRef.child("players").child("p1").child("uid").setValue(userId);

        return gameRef;
    }

    /**
     * Joins an existing game as player 2.
     * Updates the game status to 'playing'.
     * @param gameId The ID of the game to join.
     * @param userId The ID of the user joining the game.
     */
    public static void joinGame(String gameId, String userId) {
        DatabaseReference gameRef = gamesRef.child(gameId);

        gameRef.child("players").child("p2").child("uid").setValue(userId);
        gameRef.child("status").setValue("playing");
    }

    /**
     * Deletes a game instance from the database using its ID.
     * @param gameId The ID of the game to be deleted.
     */
    public static void deleteGame(String gameId) {
        gamesRef.child(gameId).removeValue();
    }
}
