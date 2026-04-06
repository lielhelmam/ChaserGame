package com.example.chasergame.screens;

import android.content.res.ColorStateList;
import android.os.Bundle;
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
import com.example.chasergame.adapters.QuestionsAdapter;
import com.example.chasergame.models.Question;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.services.GameEngine;
import com.example.chasergame.utils.GameTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayOnOneDeviceActivity extends BaseActivity {

    private static final int VISIBLE_STEPS = 14;
    private final Random rnd = new Random();

    private FrameLayout progressContainer;
    private TextView markerP1, markerP2, tvTurn, tvTimer, tvScoreP1, tvScoreP2, tvTarget, tvQuestion;
    private Button btnA, btnB, btnC;
    private int defaultBtnColor;

    private GameEngine gameEngine;
    private GameTimer gameTimer;
    private List<QuestionsAdapter.Item> questionsList;
    private String currentCorrect;
    private long turnDurationMillis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_on_one_device);

        initServices();
        initUI();
        loadData();
    }

    private void initServices() {
        gameEngine = new GameEngine(VISIBLE_STEPS);
        gameTimer = new GameTimer(new GameTimer.TimerListener() {
            @Override
            public void onTick(long ms) {
                tvTimer.setText(formatTime(ms));
            }

            @Override
            public void onFinish() {
                endTurn();
            }
        });
        turnDurationMillis = getIntent().getLongExtra("TURN_TIME_MS", 120_000);
    }

    private void initUI() {
        progressContainer = findViewById(R.id.One_Device_progressContainer);
        markerP1 = findViewById(R.id.markerP1);
        markerP2 = findViewById(R.id.markerP2);
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

        buildChaseTrackUI();
        updateMarkersUI();
        updateTurnUI();
        updateScoresUI();
        setInputsEnabled(false);
    }

    private void loadData() {
        tvQuestion.setText("Loading questions...");
        questionService.getAllQuestions(new DatabaseService.DatabaseCallback<List<QuestionsAdapter.Item>>() {
            @Override
            public void onCompleted(List<QuestionsAdapter.Item> items) {
                questionsList = items;
                if (items.isEmpty()) {
                    Toast.makeText(PlayOnOneDeviceActivity.this, "No questions", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    askReadyToStart();
                }
            }

            @Override
            public void onFailed(Exception e) {
                finish();
            }
        });
    }

    private void askReadyToStart() {
        new AlertDialog.Builder(this)
                .setTitle("Ready?")
                .setMessage("Start Player " + gameEngine.getCurrentPlayer() + " turn?")
                .setCancelable(false)
                .setPositiveButton("Yes", (d, w) -> startTurn())
                .setNegativeButton("No", (d, w) -> goHome())
                .show();
    }

    private void startTurn() {
        updateTurnUI();
        gameTimer.start(turnDurationMillis);
        loadRandomQuestion();
        setInputsEnabled(true);
    }

    private void endTurn() {
        gameTimer.cancel();
        if (gameEngine.getCurrentPlayer() == 1) {
            gameEngine.switchPlayer();
            askReadyToStart();
        } else {
            endGame("Time over!", gameEngine.getP1Score() > gameEngine.getP2Score());
        }
    }

    private void loadRandomQuestion() {
        int index = rnd.nextInt(questionsList.size());
        Question q = questionsList.get(index).value;
        showQuestion(q);
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
    }

    private void onAnswerClicked(Button btn) {
        boolean correct = btn.getText().toString().equals(currentCorrect);
        setInputsEnabled(false);
        btn.setBackgroundTintList(ColorStateList.valueOf(correct ? 0xFF2E7D32 : 0xFFC62828));

        if (correct) {
            gameEngine.onCorrectAnswer();
            updateScoresUI();
            updateMarkersUI();
        }

        btn.postDelayed(() -> {
            if (correct && gameEngine.isCaught()) {
                endGame("Player 2 caught Player 1!", false);
                return;
            }
            resetButtonColors();
            loadRandomQuestion();
            setInputsEnabled(true);
        }, correct ? 700 : 1500);
    }

    private void endGame(String msg, boolean p1Won) {
        gameTimer.cancel();
        // Removed win counting for one device games

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Main Menu", (d, w) -> navigateTo(MainActivity.class, true))
                .show();
    }

    // region UI Helpers
    private void buildChaseTrackUI() {
        LinearLayout cellsRow = new LinearLayout(this);
        cellsRow.setOrientation(LinearLayout.HORIZONTAL);
        cellsRow.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(12), Gravity.CENTER_VERTICAL));
        for (int i = 0; i < VISIBLE_STEPS; i++) {
            View cell = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(12), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            cell.setLayoutParams(lp);
            cell.setBackgroundColor((i == 0 || i == VISIBLE_STEPS - 1) ? 0xFFFFFFFF : 0xFF333333);
            cellsRow.addView(cell);
        }
        progressContainer.addView(cellsRow, 0);
    }

    private void updateMarkersUI() {
        progressContainer.post(() -> {
            float stepPx = (progressContainer.getWidth() - progressContainer.getPaddingLeft() - progressContainer.getPaddingRight()) / (float) VISIBLE_STEPS;
            markerP1.setX(progressContainer.getPaddingLeft() + (gameEngine.getP1Pos() * stepPx));
            markerP2.setX(progressContainer.getPaddingLeft() + (gameEngine.getP2Pos() * stepPx));
        });
    }

    private void updateTurnUI() {
        tvTurn.setText("Player " + gameEngine.getCurrentPlayer() + " Turn");
        tvTarget.setVisibility(gameEngine.getCurrentPlayer() == 2 ? View.VISIBLE : View.GONE);
        if (gameEngine.getCurrentPlayer() == 2) {
            tvTarget.setText("Need: " + Math.max(0, gameEngine.getP1Pos() - gameEngine.getP2Pos()));
        }
    }

    private void updateScoresUI() {
        tvScoreP1.setText("P1: " + gameEngine.getP1Score());
        tvScoreP2.setText("P2: " + gameEngine.getP2Score());
    }

    private void setInputsEnabled(boolean e) {
        btnA.setEnabled(e);
        btnB.setEnabled(e);
        btnC.setEnabled(e);
    }

    private void resetButtonColors() {
        ColorStateList c = ColorStateList.valueOf(defaultBtnColor);
        btnA.setBackgroundTintList(c);
        btnB.setBackgroundTintList(c);
        btnC.setBackgroundTintList(c);
    }

    private String formatTime(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void goHome() {
        navigateTo(MainActivity.class, true);
    }
    // endregion
}
