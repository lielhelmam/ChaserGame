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
import androidx.appcompat.app.AlertDialog;

import com.example.chasergame.R;
import com.example.chasergame.models.Question;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayOnOneDeviceActivity extends BaseActivity {

    // ===== UI =====
    private FrameLayout progressContainer;
    private View progressTrack;
    private TextView markerP1, markerP2;

    private TextView tvTurn, tvTimer, tvScoreP1, tvScoreP2, tvTarget;
    private TextView tvQuestion;
    private Button btnA, btnB, btnC;

    private int defaultBtnColor;

    // ===== Game =====
    private final Random rnd = new Random();

    private int currentPlayer = 1;            // 1 ואז 2
    private boolean isTurnRunning = false;

    private int scoreP1 = 0;
    private int scoreP2 = 0;

    // positions on the "infinite" chase track (absolute steps, not screen)
    private int p1Pos = 0;
    private int p2Pos = 0;

    // ===== Chase-style track (like "The Chase") =====
    private static final int VISIBLE_STEPS = 14; // כמה תאים רואים על המסך (תשנה 12-16 לפי טעם)
    private int trackOffset = 0;                 // כמה המסלול "המשיך" קדימה (כדי ש-P1 לא יתקע בקצה)

    // ===== Timer =====
    private CountDownTimer turnTimer;
    private long turnDurationMillis;
    private long millisLeft;

    // ===== Firebase =====
    private int questionsCount = -1;

    // current question
    private String currentCorrect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_on_one_device);



        // bind top bar
        progressContainer = findViewById(R.id.One_Device_progressContainer);
        progressTrack = findViewById(R.id.progressTrack);
        markerP1 = findViewById(R.id.markerP1);
        markerP2 = findViewById(R.id.markerP2);

        // bind labels
        tvTurn = findViewById(R.id.One_Device_tvTurn);
        tvTimer = findViewById(R.id.One_Device_tvTimer);
        tvScoreP1 = findViewById(R.id.One_Device_tvScoreP1);
        tvScoreP2 = findViewById(R.id.One_Device_tvScoreP2);
        tvTarget = findViewById(R.id.One_Device_tvTarget);

        // bind Q + answers
        tvQuestion = findViewById(R.id.Bot_tvQuestion);
        btnA = findViewById(R.id.Bot_btnAnswerA);
        btnB = findViewById(R.id.Bot_btnAnswerB);
        btnC = findViewById(R.id.Bot_btnAnswerC);

        // store default button color safely
        if (btnA.getBackgroundTintList() != null) {
            defaultBtnColor = btnA.getBackgroundTintList().getDefaultColor();
        } else {
            defaultBtnColor = 0xFF333333;
            btnA.setBackgroundTintList(ColorStateList.valueOf(defaultBtnColor));
            btnB.setBackgroundTintList(ColorStateList.valueOf(defaultBtnColor));
            btnC.setBackgroundTintList(ColorStateList.valueOf(defaultBtnColor));
        }

        btnA.setOnClickListener(v -> onAnswerClicked(btnA));
        btnB.setOnClickListener(v -> onAnswerClicked(btnB));
        btnC.setOnClickListener(v -> onAnswerClicked(btnC));

        turnDurationMillis = getIntent().getLongExtra(
                "TURN_TIME_MS",
                120_000 // ברירת מחדל אם משהו השתבש
        );

        millisLeft = turnDurationMillis;
        tvTimer.setText(formatTime(millisLeft));


        setInputsEnabled(false);
        tvQuestion.setText("Loading questions...");

        // build "cells" track like the chase
        buildChaseTrackUI();

        // after layout measured
        progressContainer.post(this::updateMarkersUI);

        // init UI
        updateScoresUI();
        updateTurnUI();
        tvTimer.setText(formatTime(120_000));

        loadQuestionsCountThenStart();
    }

    // ====== Build the "cells" progress bar (like The Chase) ======
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

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }

    // ====== Start flow ======
    private void loadQuestionsCountThenStart() {
        FirebaseDatabase.getInstance()
                .getReference("questions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        questionsCount = (int) snapshot.getChildrenCount();
                        if (questionsCount <= 0) {
                            Toast.makeText(PlayOnOneDeviceActivity.this, "No questions in database", Toast.LENGTH_LONG).show();
                            goHome();
                            return;
                        }
                        askReadyToStart();
                    }

                    @Override public void onCancelled(DatabaseError error) {
                        Toast.makeText(PlayOnOneDeviceActivity.this, "DB error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        goHome();
                    }
                });
    }

    private void askReadyToStart() {
        if (isFinishing() || isDestroyed()) return;

        new AlertDialog.Builder(this)
                .setTitle("Ready?")
                .setMessage("Start Player " + currentPlayer + " turn?")
                .setCancelable(false)
                .setPositiveButton("Yes", (d, w) -> startTurn())
                .setNegativeButton("No", (d, w) -> {
                    cancelTimer();
                    isTurnRunning = false;
                    setInputsEnabled(false);
                    goHome(); // במקום finish() שיוצא מהאפליקציה
                })
                .show();
    }

    private void startTurn() {
        isTurnRunning = true;
        millisLeft = 120_000;

        updateTurnUI();
        updateScoresUI();
        tvTimer.setText(formatTime(millisLeft));

        setInputsEnabled(true);
        millisLeft = turnDurationMillis;
        startTimer();
        loadRandomQuestion();

    }

    private void endTurn() {
        isTurnRunning = false;
        setInputsEnabled(false);
        cancelTimer();

        if (currentPlayer == 1) {
            currentPlayer = 2;
            updateTurnUI();
            askReadyToStart();
        } else {
            endGameGoHome("Time over!");
        }
    }

    // ====== Timer ======
    private void startTimer() {
        cancelTimer();

        turnTimer = new CountDownTimer(millisLeft, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                millisLeft = millisUntilFinished;
                tvTimer.setText(formatTime(millisLeft));
            }

            @Override public void onFinish() {
                millisLeft = 0;
                tvTimer.setText(formatTime(0));
                endTurn();
            }
        }.start();
    }

    private void cancelTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }

    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    // ====== Questions ======
    private void loadRandomQuestion() {
        if (!isTurnRunning) return;
        if (questionsCount <= 0) return;

        int index = rnd.nextInt(questionsCount); // 0..count-1

        FirebaseDatabase.getInstance()
                .getReference("questions")
                .child(String.valueOf(index))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        if (!isTurnRunning) return;

                        Question q = snapshot.getValue(Question.class);
                        if (q == null || q.getQuestion() == null || q.getRightAnswer() == null) {
                            loadRandomQuestion();
                            return;
                        }
                        showQuestion(q);
                    }

                    @Override public void onCancelled(DatabaseError error) {
                        if (isTurnRunning) loadRandomQuestion();
                    }
                });
    }

    private void showQuestion(Question q) {
        tvQuestion.setText(q.getQuestion());
        currentCorrect = q.getRightAnswer();

        List<String> wrongs = q.getWrongAnswers() != null ? q.getWrongAnswers() : new ArrayList<>();
        if (wrongs.size() < 2) {
            loadRandomQuestion();
            return;
        }

        List<String> options = new ArrayList<>();
        options.add(currentCorrect);
        options.add(wrongs.get(0));
        options.add(wrongs.get(1));
        Collections.shuffle(options, rnd);

        btnA.setText(options.get(0));
        btnB.setText(options.get(1));
        btnC.setText(options.get(2));

        resetAnswerButtonsColors();
    }

    // ====== Answer logic ======
    private void onAnswerClicked(Button clickedButton) {
        if (!isTurnRunning || currentCorrect == null) return;

        String chosen = clickedButton.getText().toString();
        boolean correct = chosen.equals(currentCorrect);

        setInputsEnabled(false);

        int green = 0xFF2E7D32; // dark green
        int red   = 0xFFC62828; // your theme red

        // paint clicked
        clickedButton.setBackgroundTintList(ColorStateList.valueOf(correct ? green : red));

        // if wrong: also paint the correct answer green
        if (!correct) {
            Button correctBtn = findCorrectButton();
            if (correctBtn != null) {
                correctBtn.setBackgroundTintList(ColorStateList.valueOf(green));
            }
        } else {
            // score + move only on correct
            if (currentPlayer == 1) {
                scoreP1++;
                p1Pos++;
            } else {
                scoreP2++;
                p2Pos++;
            }

            updateScoresUI();

            // chase-style: if P1 hits right edge while time remains -> scroll track
            applyChaseStyleScrolling();
            updateMarkersUI();

            // if player 2 catches player 1
            if (currentPlayer == 2 && p2Pos >= p1Pos) {
                clickedButton.postDelayed(() -> endGameGoHome("Player 2 caught Player 1!"), 700);
                return;
            }
        }

        updateTargetUI();

        // wait a bit so user sees colors, then continue
        long delay = correct ? 700 : 2000; // ✅ wrong = 3 seconds, correct = 0.7

        clickedButton.postDelayed(() -> {
            if (!isTurnRunning) return;
            resetAnswerButtonsColors();
            setInputsEnabled(true);
            loadRandomQuestion();
        }, delay);
    }

    private Button findCorrectButton() {
        if (btnA.getText().toString().equals(currentCorrect)) return btnA;
        if (btnB.getText().toString().equals(currentCorrect)) return btnB;
        if (btnC.getText().toString().equals(currentCorrect)) return btnC;
        return null;
    }

    private void resetAnswerButtonsColors() {
        ColorStateList normal = ColorStateList.valueOf(defaultBtnColor);
        btnA.setBackgroundTintList(normal);
        btnB.setBackgroundTintList(normal);
        btnC.setBackgroundTintList(normal);
    }

    // ====== The Chase track behavior ======
    private void applyChaseStyleScrolling() {
        // camera follows P1 (the runner)
        int p1ScreenPos = p1Pos - trackOffset;

        // keep P1 slightly before the right edge
        int rightMargin = 2;
        int maxScreenPos = VISIBLE_STEPS - rightMargin;

        if (p1ScreenPos > maxScreenPos) {
            int shift = p1ScreenPos - maxScreenPos;
            trackOffset += shift;
        }
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

    // ====== UI helpers ======
    private void updateTurnUI() {
        tvTurn.setText("Player " + currentPlayer + " Turn");
        updateTargetUI();
    }

    private void updateScoresUI() {
        tvScoreP1.setText("P1: " + scoreP1);
        tvScoreP2.setText("P2: " + scoreP2);
    }

    private void updateTargetUI() {
        // show gap/need like in chase (optional)
        if (currentPlayer == 2) {
            int gap = Math.max(0, p1Pos - p2Pos);
            tvTarget.setVisibility(View.VISIBLE);
            tvTarget.setText("Need: " + gap);
        } else {
            tvTarget.setVisibility(View.GONE);
        }
    }

    private void setInputsEnabled(boolean enabled) {
        btnA.setEnabled(enabled);
        btnB.setEnabled(enabled);
        btnC.setEnabled(enabled);
    }

    // ====== End / Navigation ======
    private void endGameGoHome(String reason) {
        cancelTimer();
        isTurnRunning = false;
        setInputsEnabled(false);

        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        goHome();
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}
