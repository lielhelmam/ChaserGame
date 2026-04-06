package com.example.chasergame.services;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;

public class BotService {
    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Simulates a bot's answer after a random delay.
     * The correctness of the answer is determined by the bot's accuracy.
     * @param botAccuracy The percentage chance (0-100) that the bot will answer correctly.
     * @param listener Callback to notify when the bot has "answered".
     */
    public void simulateAnswer(int botAccuracy, BotActionListener listener) {
        int botMaxDelay = 3500;
        int botMinDelay = 1500;
        int delay = botMinDelay + rnd.nextInt(botMaxDelay - botMinDelay);

        handler.postDelayed(() -> {
            boolean correct = rnd.nextInt(100) < botAccuracy;
            listener.onBotAnswer(correct);
        }, delay);
    }

    /**
     * Cancels any pending bot answer simulations.
     */
    public void cancel() {
        handler.removeCallbacksAndMessages(null);
    }

    public interface BotActionListener {
        void onBotAnswer(boolean shouldBeCorrect);
    }
}
