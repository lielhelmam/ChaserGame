package com.example.chasergame.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdapter;
import com.example.chasergame.models.Playlist;
import com.example.chasergame.models.SongData;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_details);

        playlist = (Playlist) getIntent().getSerializableExtra("PLAYLIST");
        if (playlist == null) {
            finish();
            return;
        }

        TextView tvName = findViewById(R.id.tv_playlist_name);
        tvName.setText(playlist.getName());

        RecyclerView rv = findViewById(R.id.rv_playlist_songs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongsAdapter(songs, authService, song -> playSong(song));
        rv.setAdapter(adapter);

        loadSongs();

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
        if (playlist == null) {
            Toast.makeText(this, "Error: Playlist data missing", Toast.LENGTH_LONG).show();
            return;
        }
        
        List<String> ids = playlist.getSongIds();
        if (ids == null || ids.isEmpty()) {
            Toast.makeText(this, "Playlist is empty! Add songs first.", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<String> queue = new ArrayList<>(ids);
        android.util.Log.d("PLAYLIST_DEBUG", "Queue size: " + queue.size());
        
        if (isShuffle) {
            Collections.shuffle(queue);
        }

        Intent intent = new Intent(this, RhythmGameActivity.class);
        intent.putStringArrayListExtra("SONG_QUEUE", queue);
        intent.putExtra("IS_LOOP", isLoop);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        
        Toast.makeText(this, "Launching Game with " + queue.size() + " songs", Toast.LENGTH_SHORT).show();
    }

    private void playSong(SongData song) {
        databaseService.getSongListWithKeys(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<com.example.chasergame.adapters.SongsAdminAdapter.Item> items) {
                for (com.example.chasergame.adapters.SongsAdminAdapter.Item item : items) {
                    if (item.value.getName().equals(song.getName())) {
                        Intent intent = new Intent(PlaylistDetailsActivity.this, RhythmGameActivity.class);
                        intent.putExtra("SONG_ID", item.key);
                        startActivity(intent);
                        return;
                    }
                }
                Toast.makeText(PlaylistDetailsActivity.this, "Song ID not found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(PlaylistDetailsActivity.this, "Failed to find song", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
