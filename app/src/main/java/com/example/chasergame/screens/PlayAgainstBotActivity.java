package com.example.chasergame.screens;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.example.chasergame.models.Question;
import com.google.firebase.database.*;

import java.util.*;

public class PlayAgainstBotActivity extends BaseActivity {

    // ===== UI =====
    private FrameLayout progressContainer;
    private TextView markerP1, markerP2;
    private TextView tvTurn, tvTimer, tvScoreP1, tvScoreP2, tvTarget, tvQuestion;
    private Button btnA, btnB, btnC;

    private int defaultBtnColor;

    // ===== Game =====
    private final Random rnd = new Random();
    private int currentPlayer = 1;
    private boolean isTurnRunning = false;

    private int scoreP1 = 0;
    private int scoreBot = 0;
    private View progressTrack;

    private int p1Pos = 0;

    private int p2Pos = 0;

    private int botPos = 0;

    private static final int VISIBLE_STEPS = 14;
    private int trackOffset = 0;

    // ===== Timer =====
    private CountDownTimer turnTimer;
    private long turnDurationMillis;
    private long millisLeft;

    // ===== Bot =====
    private int botAccuracy = 70;
    private int botMinDelay = 800;
    private int botMaxDelay = 1800;

    // ===== Firebase =====
    private int questionsCount = -1;
    private String currentCorrect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_on_one_device);

        // ----- bind UI -----
        progressContainer = findViewById(R.id.One_Device_progressContainer);
        markerP1 = findViewById(R.id.markerP1);
        markerP2 = findViewById(R.id.markerP2);

        progressTrack = findViewById(R.id.progressTrack);

        tvTurn = findViewById(R.id.One_Device_tvTurn);
        tvTimer = findViewById(R.id.One_Device_tvTimer);
        tvScoreP1 = findViewById(R.id.One_Device_tvScoreP1);
        tvScoreP2 = findViewById(R.id.One_Device_tvScoreP2);
        tvTarget = findViewById(R.id.One_Device_tvTarget);

        tvQuestion = findViewById(R.id.Bot_tvQuestion);
        btnA = findViewById(R.id.Bot_btnAnswerA);
        btnB = findViewById(R.id.Bot_btnAnswerB);
        btnC = findViewById(R.id.Bot_btnAnswerC);

        defaultBtnColor = btnA.getBackgroundTintList().getDefaultColor();

        btnA.setOnClickListener(v -> onAnswerClicked(btnA));
        btnB.setOnClickListener(v -> onAnswerClicked(btnB));
        btnC.setOnClickListener(v -> onAnswerClicked(btnC));

        turnDurationMillis = getIntent().getLongExtra("TURN_TIME_MS", 120_000);
        botAccuracy = getIntent().getIntExtra("BOT_ACCURACY", 70);

        millisLeft = turnDurationMillis;
        tvTimer.setText(formatTime(millisLeft));

        buildChaseTrackUI();
        progressContainer.post(this::updateMarkersUI);

        updateTurnUI();
        updateScoresUI();

        setInputsEnabled(false);
        tvQuestion.setText("Loading questions...");

        loadQuestionsCountThenStart();
    }

    // ===== Start flow =====
    private void loadQuestionsCountThenStart() {
        FirebaseDatabase.getInstance()
                .getReference("questions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        questionsCount = (int) snapshot.getChildrenCount();
                        if (questionsCount <= 0) {
                            Toast.makeText(PlayAgainstBotActivity.this, "No questions", Toast.LENGTH_LONG).show();
                            goHome();
                            return;
                        }
                        startTurn();
                    }

                    @Override public void onCancelled(DatabaseError error) {
                        goHome();
                    }
                });
    }

    private void startTurn() {
        isTurnRunning = true;
        millisLeft = turnDurationMillis;

        updateTurnUI();
        tvTimer.setText(formatTime(millisLeft));
        startTimer();
        loadRandomQuestion();

        setInputsEnabled(currentPlayer == 1);
    }

    private void endTurn() {
        cancelTimer();
        isTurnRunning = false;

        if (currentPlayer == 1) {
            currentPlayer = 2;
            startTurn();
        } else {
            endGame("Time over!");
        }
    }

    // ===== Timer =====
    private void startTimer() {
        cancelTimer();
        turnTimer = new CountDownTimer(millisLeft, 1000) {
            @Override public void onTick(long ms) {
                millisLeft = ms;
                tvTimer.setText(formatTime(ms));
            }
            @Override public void onFinish() {
                endTurn();
            }
        }.start();
    }

    private void cancelTimer() {
        if (turnTimer != null) turnTimer.cancel();
        turnTimer = null;
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    // ===== Questions =====
    private void loadRandomQuestion() {
        int index = rnd.nextInt(questionsCount);
        FirebaseDatabase.getInstance()
                .getReference("questions")
                .child(String.valueOf(index))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snap) {
                        Question q = snap.getValue(Question.class);
                        if (q != null) showQuestion(q);
                        else loadRandomQuestion();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void showQuestion(Question q) {
        tvQuestion.setText(q.getQuestion());
        currentCorrect = q.getRightAnswer();

        List<String> options = new ArrayList<>();
        options.add(currentCorrect);
        options.addAll(q.getWrongAnswers());
        Collections.shuffle(options);

        btnA.setText(options.get(0));
        btnB.setText(options.get(1));
        btnC.setText(options.get(2));

        resetButtonColors();

        if (currentPlayer == 2) simulateBotAnswer();
    }

    // ===== Bot =====
    private void simulateBotAnswer() {
        int delay = botMinDelay + rnd.nextInt(botMaxDelay - botMinDelay);
        tvQuestion.postDelayed(() -> {
            if (!isTurnRunning) return;

            boolean correct = rnd.nextInt(100) < botAccuracy;
            Button chosen = correct ? findCorrectButton() : pickWrongButton();
            if (chosen != null) onAnswerClicked(chosen);
        }, delay);
    }

    // ===== Answers =====
    private void onAnswerClicked(Button btn) {
        if (!isTurnRunning) return;

        boolean correct = btn.getText().toString().equals(currentCorrect);
        setInputsEnabled(false);

        btn.setBackgroundTintList(ColorStateList.valueOf(correct ? 0xFF2E7D32 : 0xFFC62828));

        if (correct) {
            if (currentPlayer == 1) {
                scoreP1++; p1Pos++;
            } else {
                scoreBot++; botPos++;
            }
            updateScoresUI();
            updateMarkersUI();
        }

        btn.postDelayed(() -> {
            if (correct && currentPlayer == 2 && botPos >= p1Pos) {
                endGame("Bot caught you!");
                return;
            }

            resetButtonColors();
            loadRandomQuestion();
            setInputsEnabled(currentPlayer == 1);
        }, correct ? 700 : 1500);
    }

    private Button findCorrectButton() {
        if (btnA.getText().equals(currentCorrect)) return btnA;
        if (btnB.getText().equals(currentCorrect)) return btnB;
        if (btnC.getText().equals(currentCorrect)) return btnC;
        return null;
    }

    private Button pickWrongButton() {
        List<Button> list = new ArrayList<>();
        if (!btnA.getText().equals(currentCorrect)) list.add(btnA);
        if (!btnB.getText().equals(currentCorrect)) list.add(btnB);
        if (!btnC.getText().equals(currentCorrect)) list.add(btnC);
        return list.get(rnd.nextInt(list.size()));
    }

    private void resetButtonColors() {
        ColorStateList c = ColorStateList.valueOf(defaultBtnColor);
        btnA.setBackgroundTintList(c);
        btnB.setBackgroundTintList(c);
        btnC.setBackgroundTintList(c);
    }

    // ===== UI =====
    private void updateTurnUI() {
        tvTurn.setText(currentPlayer == 1 ? "Your Turn" : "Bot Turn");
        tvTarget.setVisibility(currentPlayer == 2 ? View.VISIBLE : View.GONE);
    }

    private void updateScoresUI() {
        tvScoreP1.setText("You: " + scoreP1);
        tvScoreP2.setText("Bot: " + scoreBot);
    }

    private void setInputsEnabled(boolean e) {
        btnA.setEnabled(e);
        btnB.setEnabled(e);
        btnC.setEnabled(e);
    }

    private void buildChaseTrackUI() {
        // hide old solid track view (optional)
        if (progressTrack != null) progressTrack.setVisibility(View.GONE);

        // remove any previous dynamic track (if activity recreated)
        // (keep markers)
        // We'll add a LinearLayout as the track row.
        LinearLayout cellsRow = new LinearLayout(this);
        cellsRow.setOrientation(LinearLayout.HORIZONTAL);
        cellsRow.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(12)
        );
        lp.gravity = Gravity.CENTER_VERTICAL;
        cellsRow.setLayoutParams(lp);

        int cellColor = 0xFF333333;
        int borderColor = 0xFF111111;
        int startFinishColor = 0xFFFFFFFF;

        for (int i = 0; i < VISIBLE_STEPS; i++) {
            View cell = new View(this);

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(12), 1f);
            int m = dp(2);
            clp.setMargins(m, 0, m, 0);
            cell.setLayoutParams(clp);

            // start / finish style (white-ish), middle dark
            if (i == 0 || i == VISIBLE_STEPS) {
                cell.setBackgroundColor(startFinishColor);
            } else if (i == VISIBLE_STEPS - 1) {
                // finish cell highlight
                cell.setBackgroundColor(startFinishColor);
            } else {
                cell.setBackgroundColor(cellColor);
            }

            cellsRow.addView(cell);
        }

        // add it behind markers
        progressContainer.addView(cellsRow, 0);

        // bring markers to front
        markerP1.bringToFront();
        markerP2.bringToFront();
    }
    private void updateMarkersUI() {
        int containerW = progressContainer.getWidth();
        if (containerW <= 0) return;

        int paddingL = progressContainer.getPaddingLeft();
        int paddingR = progressContainer.getPaddingRight();
        int trackW = containerW - paddingL - paddingR;

        float stepPx = trackW / (float) VISIBLE_STEPS;

        float p1Screen = (p1Pos - trackOffset);
        float p2Screen = (p2Pos - trackOffset);

        // clamp so markers stay visible
        p1Screen = Math.max(0, Math.min(VISIBLE_STEPS, p1Screen));
        p2Screen = Math.max(0, Math.min(VISIBLE_STEPS, p2Screen));

        float x1 = paddingL + (p1Screen * stepPx);
        float x2 = paddingL + (p2Screen * stepPx);

        markerP1.setX(x1);
        markerP2.setX(x2);

        // if overlapped, lift P2 a bit
        if (Math.abs(x1 - x2) < 10f) markerP2.setTranslationY(-18f);
        else markerP2.setTranslationY(0f);
    }
    private void endGame(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        goHome();
    }
    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
