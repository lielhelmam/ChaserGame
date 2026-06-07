package com.example.chasergame.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AudioService {
    private static final String TAG = "CHASER_AUDIO_WRAPPER";
    private final Context context;
    private BackgroundAudioService backgroundService;
    private boolean isBound = false;
    
    private OnServiceBoundListener boundListener;

    public interface OnServiceBoundListener {
        void onServiceBound();
    }

    public void setOnServiceBoundListener(OnServiceBoundListener listener) {
        this.boundListener = listener;
        if (isBound && boundListener != null) {
            boundListener.onServiceBound();
        }
    }
    
    // Playback state for when service is not yet connected
    private String pendingResName;
    private AudioListener pendingListener;
    private float pendingSpeed = 1.0f;
    private boolean startPending = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected: Connected to BackgroundAudioService");
            BackgroundAudioService.LocalBinder binder = (BackgroundAudioService.LocalBinder) service;
            backgroundService = binder.getService();
            isBound = true;
            
            if (boundListener != null) {
                boundListener.onServiceBound();
            }

            if (startPending && pendingResName != null) {
                Log.i(TAG, "onServiceConnected: Executing pending request for " + pendingResName);
                playSong(pendingResName, pendingListener);
                setPlaybackSpeed(pendingSpeed);
                startPending = false;
                pendingResName = null;
                pendingListener = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected: Disconnected from BackgroundAudioService");
            backgroundService = null;
            isBound = false;
        }
    };

    public AudioService(Context context) {
        this.context = context.getApplicationContext();
        Log.i(TAG, "Initializing AudioService wrapper");
        
        Intent intent = new Intent(context, BackgroundAudioService.class);
        
        // Start the service first to ensure it lives independently of the activity
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            Log.d(TAG, "Service start command sent");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Foreground Service", e);
        }
        
        // Bind to it to allow method calls
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "bindService command sent");
    }

    public boolean isBound() {
        return isBound && backgroundService != null;
    }

    public int prepareSong(String resName) {
        Log.d(TAG, "prepareSong: Checking duration for " + resName);
        if (resName == null || resName.isEmpty()) {
            Log.e(TAG, "prepareSong: Null or empty resource name provided");
            return 0;
        }

        int resId = context.getResources().getIdentifier(resName, "raw", context.getPackageName());
        if (resId == 0) {
            Log.e(TAG, "prepareSong: Resource NOT FOUND in raw folder: " + resName);
            return 0;
        }
        
        android.media.MediaPlayer tempMp = null;
        try {
            tempMp = android.media.MediaPlayer.create(context, resId);
            if (tempMp != null) {
                int duration = tempMp.getDuration();
                Log.d(TAG, "prepareSong: Duration for " + resName + " is " + duration + "ms");
                tempMp.release();
                return duration;
            }
        } catch (Exception e) {
            Log.e(TAG, "prepareSong: Error getting duration for " + resName, e);
            if (tempMp != null) tempMp.release();
        }
        return 0;
    }

    public void playSong(String resName, AudioListener listener) {
        Log.i(TAG, "playSong: Requesting to play " + resName);
        
        if (resName == null || resName.isEmpty()) {
            Log.e(TAG, "playSong: Cannot play null/empty resource");
            return;
        }

        if (isBound && backgroundService != null) {
            int resId = context.getResources().getIdentifier(resName, "raw", context.getPackageName());
            if (resId == 0) {
                String error = "Music file not found: " + resName;
                Log.e(TAG, "playSong: " + error);
                return;
            }
            
            Log.d(TAG, "playSong: Delegating to background service for " + resName);
            backgroundService.playSong(resId, resName, new BackgroundAudioService.PlaybackListener() {
                @Override
                public void onSongCompleted() {
                    Log.i(TAG, "PlaybackListener: Song completed: " + resName);
                    if (listener != null) listener.onCompletion();
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "PlaybackListener: Error playing " + resName + ": " + message);
                    Toast.makeText(context, "Playback Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.i(TAG, "playSong: Service not bound yet, queuing " + resName);
            pendingResName = resName;
            pendingListener = listener;
            startPending = true;
        }
    }

    public int getDuration() {
        return (isBound && backgroundService != null) ? backgroundService.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return (isBound && backgroundService != null) ? backgroundService.getCurrentPosition() : 0;
    }

    public void pause() {
        if (isBound && backgroundService != null) {
            backgroundService.pause();
        }
    }

    public void resume() {
        if (isBound && backgroundService != null) {
            backgroundService.resume();
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (isBound && backgroundService != null) {
            backgroundService.setPlaybackSpeed(speed);
        } else {
            pendingSpeed = speed;
        }
    }

    public void stopService() {
        if (isBound && backgroundService != null) {
            backgroundService.stop();
        }
    }

    public String getCurrentSongName() {
        return (isBound && backgroundService != null) ? backgroundService.getCurrentSongName() : null;
    }

    public boolean isPlaying() {
        return isBound && backgroundService != null && backgroundService.isPlaying();
    }

    public void release() {
        Log.i(TAG, "release: Unbinding from background service");
        if (isBound) {
            try {
                context.unbindService(connection);
            } catch (Exception e) {
                Log.w(TAG, "release: Error during unbind", e);
            }
            isBound = false;
        }
    }

    public interface AudioListener {
        void onCompletion();
    }
}
