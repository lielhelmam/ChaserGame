package com.example.chasergame.services;

import android.content.Context;
import android.media.MediaPlayer;

public class AudioService {
    private final Context context;
    private MediaPlayer mediaPlayer;

    public AudioService(Context context) {
        this.context = context.getApplicationContext();
    }

    public int prepareSong(String resName) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        int resId = context.getResources().getIdentifier(resName, "raw", context.getPackageName());
        if (resId == 0) return 0;

        mediaPlayer = MediaPlayer.create(context, resId);
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void start(AudioListener listener) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                if (listener != null) listener.onCompletion();
            });
            mediaPlayer.start();
        }
    }

    public void playSong(String resName, AudioListener listener) {
        prepareSong(resName);
        start(listener);
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (mediaPlayer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
