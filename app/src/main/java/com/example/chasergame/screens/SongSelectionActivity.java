package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
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
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SongSelectionActivity extends BaseActivity {
    private static final String TAG = "SongSelectionActivity";

    private RecyclerView rvSongs;
    private SongsAdapter adapter;
    private List<SongData> songList;
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
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    SongData song = postSnapshot.getValue(SongData.class);
                    if (song != null) {
                        songList.add(song);
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (songList.isEmpty()) {
                    Toast.makeText(SongSelectionActivity.this, "No songs found. Add some from Admin panel!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SongSelectionActivity.this, "Failed to load songs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onSongSelected(SongData song) {
        Log.d(TAG, "Song selected: " + song.getName());
        try {
            Intent intent = new Intent(this, SecretGameActivity.class);
            String songJson = new Gson().toJson(song);
            Log.d(TAG, "Passing JSON: " + songJson);
            intent.putExtra("SONG_DATA_JSON", songJson);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting SecretGameActivity", e);
            Toast.makeText(this, "Error starting game: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
