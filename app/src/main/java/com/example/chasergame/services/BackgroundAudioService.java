package com.example.chasergame.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.chasergame.R;

public class BackgroundAudioService extends Service {

    private static final String TAG = "CHASER_AUDIO_SVC";
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private PowerManager.WakeLock wakeLock;
    private PlaybackListener currentListener;
    
    // Global state tracking
    private String currentSongName;
    private boolean isCurrentlyPlaying;

    public interface PlaybackListener {
        void onSongCompleted();
        void onError(String message);
    }

    public class LocalBinder extends Binder {
        public BackgroundAudioService getService() {
            return BackgroundAudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: BackgroundAudioService created");
        createNotificationChannel();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChaserGame:MusicWakeLock");
            Log.d(TAG, "onCreate: WakeLock initialized");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String songName = intent != null ? intent.getStringExtra("SONG_NAME") : null;
        Log.i(TAG, "onStartCommand: Received start request. Song name in intent: " + songName);
        
        startForeground(NOTIFICATION_ID, createNotification(songName != null ? "Ready: " + songName : "Ready to play"));
        
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(30 * 60 * 1000L);
            Log.d(TAG, "onStartCommand: WakeLock acquired");
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: Client bound to service");
        return binder;
    }

    public void playSong(int resId, String name, PlaybackListener listener) {
        playSongInternal(resId, null, name, listener);
    }

    public void playSongRemote(String url, String name, PlaybackListener listener) {
        playSongInternal(0, url, name, listener);
    }

    private void playSongInternal(int resId, String url, String name, PlaybackListener listener) {
        Log.i(TAG, "playSongInternal: Requesting playback of " + name + (url != null ? " (URL: " + url + ")" : " (ResID: " + resId + ")"));
        this.currentSongName = name;
        this.isCurrentlyPlaying = true;
        
        if (resId == 0 && url == null) {
            Log.e(TAG, "playSong: Invalid source (0/null). Cannot play " + name);
            if (listener != null) listener.onError("Invalid source for " + name);
            return;
        }

        if (mediaPlayer != null) {
            Log.d(TAG, "playSong: Releasing existing MediaPlayer");
            mediaPlayer.release();
        }
        
        try {
            if (url != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.i(TAG, "playSong: Remote MediaPlayer prepared successfully for " + name);
                    mp.start();
                });
            } else {
                mediaPlayer = MediaPlayer.create(this, resId);
            }

            if (mediaPlayer != null) {
                this.currentListener = listener;
                mediaPlayer.setLooping(false);
                
                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.i(TAG, "onCompletion: Song " + name + " finished");
                    if (currentListener != null) {
                        currentListener.onSongCompleted();
                    }
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    String errorMsg = "MediaPlayer Error: what=" + what + " extra=" + extra;
                    Log.e(TAG, "playSong: " + errorMsg + " for song " + name);
                    if (currentListener != null) {
                        currentListener.onError(errorMsg);
                    }
                    return false;
                });

                if (url == null) {
                    mediaPlayer.start();
                    Log.i(TAG, "playSong: MediaPlayer started playing " + name);
                }
                
                // Update notification
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, createNotification("Playing: " + name));
                }
            } else {
                Log.e(TAG, "playSong: MediaPlayer.create returned null for " + name);
                if (listener != null) listener.onError("Failed to create MediaPlayer for " + name);
            }
        } catch (Exception e) {
            Log.e(TAG, "playSong: Exception while playing " + name, e);
            if (listener != null) listener.onError("Exception: " + e.getMessage());
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isCurrentlyPlaying = false;
            Log.i(TAG, "pause: Playback paused");
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isCurrentlyPlaying = true;
            Log.i(TAG, "resume: Playback resumed");
        }
    }

    public void stop() {
        Log.i(TAG, "stop: Stopping service and playback");
        isCurrentlyPlaying = false;
        currentSongName = null;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        stopSelf();
    }

    public int getDuration() {
        int duration = (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
        Log.v(TAG, "getDuration: " + duration);
        return duration;
    }

    public int getCurrentPosition() {
        int pos = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        Log.v(TAG, "getCurrentPosition: " + pos);
        return pos;
    }

    public void setPlaybackSpeed(float speed) {
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                Log.i(TAG, "setPlaybackSpeed: Speed set to " + speed);
            } catch (Exception e) {
                Log.e(TAG, "setPlaybackSpeed: Error setting speed to " + speed, e);
            }
        }
    }

    public String getCurrentSongName() {
        return currentSongName;
    }

    public boolean isPlaying() {
        return isCurrentlyPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Chaser Rhythm")
                .setContentText(content)
                .setSmallIcon(R.drawable.icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: BackgroundAudioService being destroyed");
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
