package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SearchView;
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
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_song_selection);

        rvSongs = findViewById(R.id.rv_songs);
        searchView = findViewById(R.id.sv_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        
        songList = new ArrayList<>();
        adapter = new SongsAdapter(songList, this::onSongSelected);
        rvSongs.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("rhythm_songs");
        
        loadSongs();

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

        Button btnShop = findViewById(R.id.btn_go_to_shop);
        btnShop.setOnClickListener(v -> {
            Intent intent = new Intent(SongSelectionActivity.this, ShopActivity.class);
            startActivity(intent);
        });

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
                adapter.updateList(songList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SongSelectionActivity.this, "Failed to load songs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onSongSelected(SongData song) {
        String songId = songKeys.get(song);
        if (songId != null) {
            Intent intent = new Intent(SongSelectionActivity.this, SecretGameActivity.class);
            intent.putExtra("SONG_ID", songId);
            startActivity(intent);
        }
    }
}
