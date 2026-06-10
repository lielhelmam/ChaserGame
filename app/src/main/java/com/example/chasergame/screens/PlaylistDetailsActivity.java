package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdapter;
import com.example.chasergame.models.Playlist;
import com.example.chasergame.models.SongData;
import com.example.chasergame.services.AudioService;
import com.example.chasergame.services.DatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistDetailsActivity extends BaseActivity {

    private Playlist playlist;
    private List<SongData> songs = new ArrayList<>();
    private SongsAdapter adapter;
    private boolean isShuffle = false;
    private boolean isLoop = false;

    // Spotify-style playback fields
    private AudioService audioService;
    private List<SongData> playbackQueue = new ArrayList<>();
    private int currentPlayingIndex = -1;
    private boolean isPlaying = false;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // Mini Player Views
    private View miniPlayerContainer;
    private TextView tvMiniSongName;
    private ImageButton btnPlayPause;
    private ProgressBar pbProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);

        playlist = (Playlist) getIntent().getSerializableExtra("PLAYLIST");
        if (playlist == null) {
            finish();
            return;
        }

        audioService = new AudioService(this);

        TextView tvName = findViewById(R.id.tv_playlist_name);
        tvName.setText(playlist.getName());

        RecyclerView rv = findViewById(R.id.rv_playlist_songs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongsAdapter(songs, authService, song -> {
            // In Spotify-style, clicking a song in the list plays it in the background
            playSpecificSong(song);
        });
        rv.setAdapter(adapter);

        loadSongs();
        initMiniPlayer();

        SearchView searchView = findViewById(R.id.sv_playlist_songs);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        ImageButton btnShuffle = findViewById(R.id.btn_shuffle);
        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setColorFilter(isShuffle ? 0xFF1DB954 : 0xFF888888);
        });

        ImageButton btnLoop = findViewById(R.id.btn_loop);
        btnLoop.setOnClickListener(v -> {
            isLoop = !isLoop;
            btnLoop.setColorFilter(isLoop ? 0xFF1DB954 : 0xFF888888);
        });

        findViewById(R.id.btn_play_playlist).setOnClickListener(v -> {
            android.util.Log.d("PLAYLIST_DEBUG", "Play button clicked");
            Toast.makeText(this, "Starting Playlist...", Toast.LENGTH_SHORT).show();
            playPlaylist();
        });
    }

    private void loadSongs() {
        databaseService.getSongListWithKeys(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<com.example.chasergame.adapters.SongsAdminAdapter.Item> items) {
                songs.clear();
                for (String id : playlist.getSongIds()) {
                    for (com.example.chasergame.adapters.SongsAdminAdapter.Item item : items) {
                        if (item.key.equals(id)) {
                            songs.add(item.value);
                            break;
                        }
                    }
                }
                adapter.updateList(songs);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(PlaylistDetailsActivity.this, "Failed to load songs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playPlaylist() {
        if (songs.isEmpty()) {
            Toast.makeText(this, "Playlist is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        playbackQueue = new ArrayList<>(songs);
        if (isShuffle) {
            Collections.shuffle(playbackQueue);
        }
        currentPlayingIndex = 0;
        startPlayback(playbackQueue.get(currentPlayingIndex));
    }

    private void playSpecificSong(SongData song) {
        playbackQueue = new ArrayList<>(songs);
        currentPlayingIndex = playbackQueue.indexOf(song);
        if (currentPlayingIndex != -1) {
            startPlayback(song);
        }
    }

    private void startPlayback(SongData song) {
        if (song == null) return;

        isPlaying = true;
        updateMiniPlayerUI(song);
        miniPlayerContainer.setVisibility(View.VISIBLE);

        audioService.playSong(song.getResName(), () -> {
            // On song completion, play next
            playNext();
        });

        startProgressUpdates();
    }

    private void playNext() {
        if (playbackQueue.isEmpty()) return;
        currentPlayingIndex++;
        if (currentPlayingIndex >= playbackQueue.size()) {
            if (isLoop) {
                currentPlayingIndex = 0;
            } else {
                currentPlayingIndex = -1;
                stopPlayback();
                return;
            }
        }
        startPlayback(playbackQueue.get(currentPlayingIndex));
    }

    private void playPrevious() {
        if (playbackQueue.isEmpty()) return;
        currentPlayingIndex--;
        if (currentPlayingIndex < 0) {
            currentPlayingIndex = isLoop ? playbackQueue.size() - 1 : 0;
        }
        startPlayback(playbackQueue.get(currentPlayingIndex));
    }

    private void togglePlayPause() {
        if (isPlaying) {
            audioService.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            isPlaying = false;
        } else {
            audioService.resume();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            isPlaying = true;
        }
    }

    private void stopPlayback() {
        isPlaying = false;
        miniPlayerContainer.setVisibility(View.GONE);
        stopProgressUpdates();
    }

    private void initMiniPlayer() {
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        tvMiniSongName = findViewById(R.id.tv_mini_player_song_name);
        btnPlayPause = findViewById(R.id.btn_mini_player_play_pause);
        pbProgress = findViewById(R.id.pb_mini_player_progress);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        findViewById(R.id.btn_mini_player_next).setOnClickListener(v -> playNext());
        findViewById(R.id.btn_mini_player_prev).setOnClickListener(v -> playPrevious());

        // Also add a long click to launch the game?
        miniPlayerContainer.setOnLongClickListener(v -> {
            if (currentPlayingIndex != -1) {
                launchGameWithCurrentSong();
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (audioService != null) {
            audioService.setOnServiceBoundListener(this::restorePlayerState);
        }
    }

    private void restorePlayerState() {
        if (audioService != null && audioService.getCurrentSongName() != null) {
            String playingName = audioService.getCurrentSongName();
            // Find in current playlist
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).getName().equals(playingName)) {
                    playbackQueue = new ArrayList<>(songs);
                    currentPlayingIndex = i;
                    isPlaying = audioService.isPlaying();
                    
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                    tvMiniSongName.setText(playingName);
                    btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    
                    startProgressUpdates();
                    return;
                }
            }
        }
    }

    private void launchGameWithCurrentSong() {
        SongData currentSong = playbackQueue.get(currentPlayingIndex);
        // We need the ID for the game
        databaseService.getSongListWithKeys(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<com.example.chasergame.adapters.SongsAdminAdapter.Item> items) {
                for (com.example.chasergame.adapters.SongsAdminAdapter.Item item : items) {
                    if (item.value.getName().equals(currentSong.getName())) {
                        Intent intent = new Intent(PlaylistDetailsActivity.this, RhythmGameActivity.class);
                        intent.putExtra("SONG_ID", item.key);
                        startActivity(intent);
                        return;
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {}
        });
    }

    private void updateMiniPlayerUI(SongData song) {
        tvMiniSongName.setText(song.getName());
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void startProgressUpdates() {
        stopProgressUpdates();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    int pos = audioService.getCurrentPosition();
                    int duration = audioService.getDuration();
                    if (duration > 0) {
                        pbProgress.setMax(duration);
                        pbProgress.setProgress(pos);
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        if (audioService != null) {
            audioService.release();
        }
    }
}
