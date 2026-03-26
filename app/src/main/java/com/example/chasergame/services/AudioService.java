package com.example.chasergame.services;

import android.content.Context;
import android.media.MediaPlayer;

public class AudioService {
    private final Context context;
    private MediaPlayer mediaPlayer;

    public AudioService(Context context) {
        this.context = context.getApplicationContext();
    }

    public void playSong(String resName, AudioListener listener) {
        int resId = context.getResources().getIdentifier(resName, "raw", context.getPackageName());
        if (resId == 0) return;

        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = MediaPlayer.create(context, resId);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> listener.onCompletion());
            mediaPlayer.start();
        }
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public interface AudioListener {
        void onCompletion();
    }
}
