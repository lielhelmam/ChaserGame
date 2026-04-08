package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdapter;
import com.example.chasergame.adapters.SongsAdminAdapter;
import com.example.chasergame.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class SongSelectionActivity extends BaseActivity {

    private SongsAdapter adapter;
    private List<SongsAdminAdapter.Item> songItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_song_selection);

        RecyclerView rvSongs = findViewById(R.id.rv_songs);
        SearchView searchView = findViewById(R.id.sv_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongsAdapter(new ArrayList<>(), song -> {
            // Find the key for the selected song
            for (SongsAdminAdapter.Item item : songItems) {
                if (item.value.equals(song)) {
                    Intent intent = new Intent(this, RhythmGameActivity.class);
                    intent.putExtra("SONG_ID", item.key);
                    startActivity(intent);
                    break;
                }
            }
        });
        rvSongs.setAdapter(adapter);

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

        findViewById(R.id.btn_go_to_shop).setOnClickListener(v -> navigateTo(ShopActivity.class, false));
        findViewById(R.id.btn_back_to_rules).setOnClickListener(v -> navigateTo(RhythmGameRulesActivity.class, false));
    }

    private void loadSongs() {
        databaseService.getSongListWithKeys(new DatabaseService.DatabaseCallback<List<SongsAdminAdapter.Item>>() {
            @Override
            public void onCompleted(List<SongsAdminAdapter.Item> items) {
                songItems = items;
                List<com.example.chasergame.models.SongData> songs = new ArrayList<>();
                for (SongsAdminAdapter.Item item : items) songs.add(item.value);
                adapter.updateList(songs);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(SongSelectionActivity.this, "Failed to load songs.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
