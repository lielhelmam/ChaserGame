package com.example.chasergame.services;

import androidx.annotation.NonNull;

import com.example.chasergame.models.Question;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class OnlineGameService {
    private static long serverTimeOffsetMs = 0;
    private static boolean isTimeSynced = false;
    private final DatabaseReference roomRef;
    private final DatabaseReference gameRef;
    private final String playerId;
    private ValueEventListener gameListener;
    private ValueEventListener timeOffsetListener;

    /**
     * Constructor for OnlineGameService.
     * Initializes database references and starts syncing with server time.
     *
     * @param roomId   The ID of the game room.
     * @param playerId The ID of the current player.
     */
    public OnlineGameService(String roomId, String playerId) {
        this.playerId = playerId;
        this.roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);
        this.gameRef = roomRef.child("game");
        listenToServerTime();
    }

    /**
     * Checks if the server time offset has been successfully synced.
     *
     * @return true if time is synced, false otherwise.
     */
    public static boolean isTimeSynced() {
        return isTimeSynced;
    }

    /**
     * Sets up a listener to sync the local time with Firebase server time offset.
     */
    private void listenToServerTime() {
        if (isTimeSynced) return; // Already listening or synced
        timeOffsetListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                Long offset = s.getValue(Long.class);
                if (offset != null) {
                    serverTimeOffsetMs = offset;
                    isTimeSynced = true;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").addValueEventListener(timeOffsetListener);
    }

    /**
     * Calculates the current server time based on the local time and synced offset.
     *
     * @return Current server time in milliseconds.
     */
    public long getServerTime() {
        return System.currentTimeMillis() + serverTimeOffsetMs;
    }

    /**
     * Attaches a listener to the game state in the database.
     * Notifies the listener about game state changes or game over events.
     *
     * @param listener The callback listener for game state events.
     */
    public void startListening(GameStateListener listener) {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                Boolean isOver = snapshot.child("gameOver").getValue(Boolean.class);
                if (isOver != null && isOver) {
                    listener.onGameOver(snapshot.child("winner").getValue(String.class));
                } else {
                    listener.onGameStateChanged(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        };
        gameRef.addValueEventListener(gameListener);
    }

    /**
     * Removes all active database listeners to prevent memory leaks and unnecessary updates.
     */
    public void stopListening() {
        if (gameListener != null) {
            gameRef.removeEventListener(gameListener);
        }
        if (timeOffsetListener != null) {
            FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").removeEventListener(timeOffsetListener);
        }
    }

    /**
     * Initializes the game state in the database with default values.
     * Sets the first turn, initial scores, and prepares the first question.
     *
     * @param defaultTurnMs   Default duration for a turn in milliseconds.
     * @param questionService Service used to fetch questions.
     */
    public void initializeGame(long defaultTurnMs, QuestionService questionService) {
        Map<String, Object> initial = new HashMap<>();
        initial.put("currentTurn", "p1");
        initial.put("p1Score", 0);
        initial.put("p2Score", 0);
        initial.put("turnStartedAt", ServerValue.TIMESTAMP);
        initial.put("turnDurationMs", defaultTurnMs);
        initial.put("gameOver", false);

        gameRef.setValue(initial).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                setNewQuestion(questionService);
            }
        });
    }

    /**
     * Validates a player's answer against the correct answer stored in the database.
     *
     * @param key      The answer key selected by the player.
     * @param callback Callback to return the result of the validation.
     */
    public void validateAnswer(String key, AnswerCallback callback) {
        gameRef.child("question/correct").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String correct = snapshot.getValue(String.class);
                callback.onResult(key.equals(correct), correct);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Increments the player's score in the database and triggers a new question.
     * Uses a transaction to ensure atomic score updates.
     *
     * @param playerKey       The key identifying the player (e.g., "p1" or "p2").
     * @param questionService Service to fetch a new question.
     * @param callback        Optional callback for completion or failure.
     */
    public void submitAnswer(String playerKey, QuestionService questionService, DatabaseService.DatabaseCallback<Void> callback) {
        gameRef.child(playerKey + "Score").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                Long current = d.getValue(Long.class);
                d.setValue((current == null ? 0 : current) + 1);
                return Transaction.success(d);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot s) {
                if (e == null) {
                    setNewQuestion(questionService);
                    if (callback != null) callback.onCompleted(null);
                } else {
                    if (callback != null) callback.onFailed(e.toException());
                }
            }
        });
    }

    /**
     * Handles the end of a turn, switching to the next player or ending the game.
     * Calculates the winner if the game is finished.
     *
     * @param defaultTurnMs Duration for the next turn.
     */
    public void endTurn(long defaultTurnMs) {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                String turn = d.child("currentTurn").getValue(String.class);
                if ("p1".equals(turn)) {
                    d.child("currentTurn").setValue("p2");
                    d.child("turnStartedAt").setValue(ServerValue.TIMESTAMP);
                    d.child("turnDurationMs").setValue(defaultTurnMs);
                } else {
                    d.child("gameOver").setValue(true);
                    long p1 = getLong(d.child("p1Score"));
                    long p2 = getLong(d.child("p2Score"));
                    d.child("winner").setValue(p1 > p2 ? "P1" : (p2 > p1 ? "P2" : "DRAW"));
                }
                return Transaction.success(d);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot s) {
            }
        });
    }

    /**
     * Safely retrieves a long value from MutableData.
     *
     * @param d The MutableData containing the potential number.
     * @return The long value, or 0L if invalid.
     */
    private long getLong(MutableData d) {
        Object v = d.getValue();
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    /**
     * Fetches a new random question and updates the game state in the database.
     * Shuffles the answers before uploading.
     *
     * @param questionService Service used to retrieve questions.
     */
    public void setNewQuestion(QuestionService questionService) {
        questionService.getAllQuestions(new DatabaseService.DatabaseCallback<List<com.example.chasergame.adapters.QuestionsAdapter.Item>>() {
            @Override
            public void onCompleted(List<com.example.chasergame.adapters.QuestionsAdapter.Item> items) {
                if (items.isEmpty()) return;
                Question q = items.get(new Random().nextInt(items.size())).value;

                List<String> all = new ArrayList<>(q.getWrongAnswers());
                all.add(q.getRightAnswer());
                Collections.shuffle(all);

                Map<String, Object> qm = new HashMap<>();
                qm.put("nonce", System.currentTimeMillis());
                qm.put("text", q.getQuestion());
                qm.put("a", all.get(0));
                qm.put("b", all.get(1));
                qm.put("c", all.get(2));
                qm.put("correct", all.indexOf(q.getRightAnswer()) == 0 ? "A" : (all.indexOf(q.getRightAnswer()) == 1 ? "B" : "C"));

                gameRef.child("question").setValue(qm);
            }

            @Override
            public void onFailed(Exception e) {
            }
        });
    }

    /**
     * Removes the entire game room from the database.
     */
    public void deleteRoom() {
        roomRef.removeValue();
    }

    public interface GameStateListener {
        void onGameStateChanged(DataSnapshot snapshot);

        void onGameOver(String winner);

        void onError(Exception e);
    }

    public interface AnswerCallback {
        void onResult(boolean isCorrect, String correctAnswer);
    }
}
