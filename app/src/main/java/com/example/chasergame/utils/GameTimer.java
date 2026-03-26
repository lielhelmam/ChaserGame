package com.example.chasergame.utils;

import android.os.CountDownTimer;

public class GameTimer {
    private final TimerListener listener;
    private CountDownTimer timer;

    public GameTimer(TimerListener listener) {
        this.listener = listener;
    }

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
