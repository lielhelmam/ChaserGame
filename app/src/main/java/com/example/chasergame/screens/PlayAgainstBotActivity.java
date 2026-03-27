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
import com.example.chasergame.services.BotService;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.services.GameEngine;
import com.example.chasergame.utils.GameTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayAgainstBotActivity extends BaseActivity {

    private static final int VISIBLE_STEPS = 14;
    private final Random rnd = new Random();

    private FrameLayout progressContainer;
    private TextView markerP1, markerP2, tvTurn, tvTimer, tvScoreP1, tvScoreP2, tvTarget, tvQuestion;
    private Button btnA, btnB, btnC;
    private int defaultBtnColor;

    private GameEngine gameEngine;
    private BotService botService;
    private GameTimer gameTimer;

    private List<QuestionsAdapter.Item> questionsList;
    private String currentCorrect;
    private int botAccuracy;
    private long turnDurationMillis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_against_bot);

        initServices();
        initUI();
        loadData();
    }

    private void initServices() {
        gameEngine = new GameEngine(VISIBLE_STEPS);
        botService = new BotService();
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
        botAccuracy = getIntent().getIntExtra("BOT_ACCURACY", 70);
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
                    Toast.makeText(PlayAgainstBotActivity.this, "No questions", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    startTurn();
                }
            }

            @Override
            public void onFailed(Exception e) {
                finish();
            }
        });
    }

    private void startTurn() {
        updateTurnUI();
        gameTimer.start(turnDurationMillis);
        loadRandomQuestion();
        setInputsEnabled(gameEngine.getCurrentPlayer() == 1);
    }

    private void endTurn() {
        gameTimer.cancel();
        if (gameEngine.getCurrentPlayer() == 1) {
            gameEngine.switchPlayer();
            startTurn();
        } else {
            endGame("Time is up! You survived!", true);
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
        if (gameEngine.getCurrentPlayer() == 2) simulateBotAnswer();
    }

    private void simulateBotAnswer() {
        botService.simulateAnswer(botAccuracy, correct -> {
            Button chosen = correct ? findCorrectButton() : pickWrongButton();
            if (chosen != null) onAnswerClicked(chosen);
        });
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
                endGame("Caught by the bot!", false);
                return;
            }
            resetButtonColors();
            loadRandomQuestion();
            setInputsEnabled(gameEngine.getCurrentPlayer() == 1);
        }, correct ? 700 : 1500);
    }

    private void endGame(String msg, boolean playerWon) {
        gameTimer.cancel();
        botService.cancel();

        if (playerWon) {
            User user = authService.getCurrentUser();
            if (user != null) {
                String winField = botAccuracy < 50 ? "botWinsEasy" : (botAccuracy < 75 ? "botWinsNormal" : "botWinsHard");

                if ("botWinsEasy".equals(winField)) user.setBotWinsEasy(user.getBotWinsEasy() + 1);
                else if ("botWinsNormal".equals(winField)) user.setBotWinsNormal(user.getBotWinsNormal() + 1);
                else user.setBotWinsHard(user.getBotWinsHard() + 1);

                databaseService.updateUserWins(user.getId(), winField, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void unused) {
                        authService.syncUser(user);
                    }

                    @Override
                    public void onFailed(Exception e) {
                    }
                });
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(playerWon ? "Victory!" : "Game Over")
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
        tvTurn.setText(gameEngine.getCurrentPlayer() == 1 ? "Your Turn" : "Bot Turn");
        tvTarget.setVisibility(gameEngine.getCurrentPlayer() == 2 ? View.VISIBLE : View.GONE);
    }

    private void updateScoresUI() {
        tvScoreP1.setText("You: " + gameEngine.getP1Score());
        tvScoreP2.setText("Bot: " + gameEngine.getP2Score());
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

    private Button findCorrectButton() {
        if (btnA.getText().equals(currentCorrect)) return btnA;
        if (btnB.getText().equals(currentCorrect)) return btnB;
        return btnC;
    }

    private Button pickWrongButton() {
        List<Button> list = new ArrayList<>();
        if (!btnA.getText().equals(currentCorrect)) list.add(btnA);
        if (!btnB.getText().equals(currentCorrect)) list.add(btnB);
        if (!btnC.getText().equals(currentCorrect)) list.add(btnC);
        return list.get(rnd.nextInt(list.size()));
    }
    // endregion
}
