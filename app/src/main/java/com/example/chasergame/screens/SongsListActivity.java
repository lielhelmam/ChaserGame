package com.example.chasergame.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Spinner;
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
        Spinner spDifficulty = v.findViewById(R.id.sp_edit_song_difficulty);
        EditText etResId = v.findViewById(R.id.et_edit_song_res_id);
        EditText etHpDrain = v.findViewById(R.id.et_edit_hp_drain);
        EditText etHpGain = v.findViewById(R.id.et_edit_hp_gain);
        EditText etBpm = v.findViewById(R.id.et_edit_song_bpm);

        // Setup Spinner
        String[] levels = {
                "Beginner (40-90)",
                "Easy (80-120)",
                "Medium (100-150)",
                "Hard (130-180)",
                "Insane (160-220)",
                "Expert (200-300)"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDifficulty.setAdapter(adapter);

        // Pre-fill data
        etName.setText(song.getName());
        etResId.setText(song.getResName());
        etHpDrain.setText(String.valueOf(song.getHpDrain()));
        etHpGain.setText(String.valueOf(song.getHpGain()));
        etBpm.setText(String.valueOf(song.getBpm()));

        // Set spinner selection
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].toLowerCase().startsWith(song.getDifficulty().toLowerCase())) {
                spDifficulty.setSelection(i);
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Song")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String selectedDifficulty = spDifficulty.getSelectedItem().toString();
                    // Extract base difficulty name
                    String difficulty = selectedDifficulty.split(" ")[0];

                    String resName = etResId.getText().toString().trim();
                    String drainStr = etHpDrain.getText().toString().trim();
                    String gainStr = etHpGain.getText().toString().trim();
                    String bpmStr = etBpm.getText().toString().trim();

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(resName) ||
                            TextUtils.isEmpty(drainStr) || TextUtils.isEmpty(gainStr) || TextUtils.isEmpty(bpmStr)) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int hpDrain, hpGain, bpm;
                    try {
                        hpDrain = Integer.parseInt(drainStr);
                        hpGain = Integer.parseInt(gainStr);
                        bpm = Integer.parseInt(bpmStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validate BPM
                    if (!isBpmValid(bpm, difficulty)) return;

                    songService.updateSong(key, name, difficulty, resName, bpm, hpDrain, hpGain, new DatabaseService.DatabaseCallback<Void>() {
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

    private boolean isBpmValid(int bpm, String difficulty) {
        int min, max;
        switch (difficulty.toLowerCase()) {
            case "beginner":
                min = 40;
                max = 90;
                break;
            case "easy":
                min = 80;
                max = 120;
                break;
            case "medium":
                min = 100;
                max = 150;
                break;
            case "hard":
                min = 130;
                max = 180;
                break;
            case "insane":
                min = 160;
                max = 220;
                break;
            case "expert":
                min = 200;
                max = 300;
                break;
            default:
                min = 40;
                max = 300;
        }
        if (bpm < min || bpm > max) {
            Toast.makeText(this, "BPM for " + difficulty + " must be between " + min + " and " + max, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
