package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdapter;
import com.example.chasergame.models.SongData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SongSelectionActivity extends BaseActivity {
    private static final String TAG = "SongSelectionActivity";

    private RecyclerView rvSongs;
    private SongsAdapter adapter;
    private List<SongData> songList;
    private Map<SongData, String> songKeys = new HashMap<>(); 
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_song_selection);

        rvSongs = findViewById(R.id.rv_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        
        songList = new ArrayList<>();
        adapter = new SongsAdapter(songList, this::onSongSelected);
        rvSongs.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("rhythm_songs");
        
        loadSongs();

        findViewById(R.id.btn_back_to_rules).setOnClickListener(v -> finish());
    }

    private void loadSongs() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                songList.clear();
                songKeys.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    SongData song = postSnapshot.getValue(SongData.class);
                    if (song != null) {
                        String key = postSnapshot.getKey();
                        songList.add(song);
                        songKeys.put(song, key);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SongSelectionActivity.this, "Failed to load songs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onSongSelected(SongData song) {
        String songId = songKeys.get(song);
        Log.d(TAG, "onSongSelected: Song=" + song.getName() + ", ID=" + songId);
        
        if (songId != null) {
            Toast.makeText(this, "Starting: " + song.getName(), Toast.LENGTH_SHORT).show();
            try {
                Intent intent = new Intent(SongSelectionActivity.this, SecretGameActivity.class);
                intent.putExtra("SONG_ID", songId);
                startActivity(intent);
                Log.d(TAG, "startActivity called for ID: " + songId);
            } catch (Exception e) {
                Log.e(TAG, "Error starting Activity", e);
                Toast.makeText(this, "Launch failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Error: Missing song ID", Toast.LENGTH_SHORT).show();
        }
    }
}
