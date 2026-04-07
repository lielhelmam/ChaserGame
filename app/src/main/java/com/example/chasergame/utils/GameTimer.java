package com.example.chasergame.utils;

import android.os.CountDownTimer;

public class GameTimer {
    private final TimerListener listener;
    private CountDownTimer timer;

    /**
     * Constructor for GameTimer.
     *
     * @param listener The listener to be notified of timer events.
     */
    public GameTimer(TimerListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the countdown timer.
     * If a timer is already running, it will be cancelled before starting the new one.
     *
     * @param durationMillis The duration of the timer in milliseconds.
     */
    public void start(long durationMillis) {
        cancel();
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                listener.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                listener.onFinish();
            }
        }.start();
    }

    /**
     * Cancels the currently running timer if it exists.
     */
    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public interface TimerListener {
        void onTick(long millisUntilFinished);

        void onFinish();
    }
}
