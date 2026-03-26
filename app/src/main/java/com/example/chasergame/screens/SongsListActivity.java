package com.example.chasergame.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdminAdapter;
import com.example.chasergame.models.SongData;
import com.example.chasergame.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class SongsListActivity extends BaseActivity {

    private SongsAdminAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs_list);

        Toolbar toolbar = findViewById(R.id.toolbarSongs);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupSearchView();
        loadSongs();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rv_songs_list);
        adapter = new SongsAdminAdapter(new SongsAdminAdapter.Listener() {
            @Override
            public void onEditClicked(String key, SongData song) {
                showEditDialog(key, song);
            }

            @Override
            public void onDeleteClicked(String key) {
                confirmDelete(key);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchViewSongs);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void loadSongs() {
        songService.getAllSongs(new DatabaseService.DatabaseCallback<List<SongData>>() {
            @Override
            public void onCompleted(List<SongData> songs) {
                List<SongsAdminAdapter.Item> items = new ArrayList<>();
                // If we need keys, we should probably change ISongRepository to return Items with keys
                // For now, assuming SongData might have an ID or using the list index if keys are not stored in model
                // However, in Firebase, keys are separate. Let's assume the previous logic.
                // Re-fetching from DatabaseService with keys if necessary.

                // Let's call a more specific method or update DatabaseService to provide keys with SongData
                // For simplicity in this step, I'll update DatabaseService's getSongList to return Items.
                databaseService.getSongListWithKeys(new DatabaseService.DatabaseCallback<List<SongsAdminAdapter.Item>>() {
                    @Override
                    public void onCompleted(List<SongsAdminAdapter.Item> items) {
                        adapter.setItems(items);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(SongsListActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(SongsListActivity.this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(String key) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete this song?")
                .setPositiveButton("Delete", (d, w) -> {
                    songService.deleteSong(key, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(SongsListActivity.this, "Song deleted.", Toast.LENGTH_SHORT).show();
                            loadSongs();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(SongsListActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(String key, SongData song) {
        android.view.View v = getLayoutInflater().inflate(R.layout.dialog_edit_song, null);

        EditText etName = v.findViewById(R.id.et_edit_song_name);
        EditText etDifficulty = v.findViewById(R.id.et_edit_song_difficulty);
        EditText etResId = v.findViewById(R.id.et_edit_song_res_id);
        EditText etTargetScore = v.findViewById(R.id.et_edit_target_score);
        EditText etBeatmap = v.findViewById(R.id.et_edit_beatmap);

        etName.setText(song.getName());
        etDifficulty.setText(song.getDifficulty());
        etResId.setText(song.getResName());
        etTargetScore.setText(String.valueOf(song.getTargetScore()));
        etBeatmap.setText(songService.notesToString(song.getNotes()));

        new AlertDialog.Builder(this)
                .setTitle("Edit Song")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String difficulty = etDifficulty.getText().toString().trim();
                    String resName = etResId.getText().toString().trim();
                    String scoreStr = etTargetScore.getText().toString().trim();
                    String beatmapStr = etBeatmap.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(difficulty) || TextUtils.isEmpty(resName) ||
                            TextUtils.isEmpty(scoreStr) || TextUtils.isEmpty(beatmapStr)) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int targetScore = Integer.parseInt(scoreStr);

                    songService.updateSong(key, name, difficulty, resName, targetScore, beatmapStr, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(SongsListActivity.this, "Song updated.", Toast.LENGTH_SHORT).show();
                            loadSongs();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(SongsListActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
