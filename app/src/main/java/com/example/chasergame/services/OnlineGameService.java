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
    private final DatabaseReference roomRef;
    private final DatabaseReference gameRef;
    private final String playerId;
    private ValueEventListener gameListener;

    public OnlineGameService(String roomId, String playerId) {
        this.playerId = playerId;
        this.roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);
        this.gameRef = roomRef.child("game");
    }

    public void startListening(GameStateListener listener) {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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

    public void stopListening() {
        if (gameListener != null) {
            gameRef.removeEventListener(gameListener);
        }
    }

    public void submitAnswer(String playerKey, boolean correct, QuestionService questionService, DatabaseService.DatabaseCallback<Void> callback) {
        if (correct) {
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
                        callback.onCompleted(null);
                    } else callback.onFailed(e.toException());
                }
            });
        }
    }

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

    private long getLong(MutableData d) {
        Object v = d.getValue();
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

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

    public void deleteRoom() {
        roomRef.removeValue();
    }

    public interface GameStateListener {
        void onGameStateChanged(DataSnapshot snapshot);

        void onGameOver(String winner);

        void onError(Exception e);
    }
}
