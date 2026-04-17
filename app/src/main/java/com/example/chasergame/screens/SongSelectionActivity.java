package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdapter;
import com.example.chasergame.models.SongData;
import com.example.chasergame.services.DatabaseService;
import com.google.firebase.database.DataSnapshot;

public class SongSelectionActivity extends BaseActivity {

    private SongsAdapter adapter;
    private List<DataSnapshot> songSnapshots = new ArrayList<>();
    
    // Mods logic
    private final String[] modNames = {"Neural Glitch (1.5x)", "Gravity Warp (1.3x)", "Blind (1.8x)", "Dual Stream (1.4x)", "Static (1.2x)", "Overclocked (2.0x)"};
    private final String[] modKeys = {"GLITCH", "GRAVITY", "BLIND", "DUAL", "STATIC", "OVERCLOCK"};
    private final double[] modMultipliers = {1.5, 1.3, 1.8, 1.4, 1.2, 2.0};
    private final boolean[] selectedMods = new boolean[modNames.length];
    private Set<String> activeMods = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_song_selection);

        RecyclerView rvSongs = findViewById(R.id.rv_songs);
        SearchView searchView = findViewById(R.id.sv_songs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongsAdapter(new ArrayList<>(), authService, song -> {
            // Find the key for the selected song
            for (DataSnapshot snap : songSnapshots) {
                SongData sd = snap.getValue(SongData.class);
                if (sd != null && sd.getName().equals(song.getName())) {
                    Intent intent = new Intent(this, RhythmGameActivity.class);
                    intent.putExtra("SONG_ID", snap.getKey());
                    intent.putStringArrayListExtra("ACTIVE_MODS", new ArrayList<>(activeMods));
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

        findViewById(R.id.btn_go_to_shop).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btn_game_mods).setOnClickListener(v -> showModsDialog());
        
        findViewById(R.id.btn_back_to_rules).setOnClickListener(v -> {
            Intent intent = new Intent(this, RhythmGameRulesActivity.class);
            startActivity(intent);
        });
    }

    private void showModsDialog() {
        String[] descriptions = {
                "Visual distortion effects",
                "Notes fall at varying speeds",
                "Hidden notes until they are close",
                "Double the notes to process",
                "Screen noise interference",
                "Insane game speed"
        };

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_game_mods, null);
        RecyclerView rvMods = dialogView.findViewById(R.id.rv_mods_list);
        rvMods.setLayoutManager(new LinearLayoutManager(this));
        rvMods.setAdapter(new com.example.chasergame.adapters.ModsAdapter(modNames, descriptions, modKeys, selectedMods, activeMods));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btn_mods_reset).setOnClickListener(v -> {
            for (int i = 0; i < selectedMods.length; i++) selectedMods[i] = false;
            activeMods.clear();
            rvMods.getAdapter().notifyDataSetChanged();
            Toast.makeText(this, "Systems Restored", Toast.LENGTH_SHORT).show();
        });

        dialogView.findViewById(R.id.btn_mods_activate).setOnClickListener(v -> {
            double totalMult = 1.0;
            for (int i = 0; i < selectedMods.length; i++) {
                if (selectedMods[i]) totalMult *= modMultipliers[i];
            }
            if (totalMult > 1.0) {
                Toast.makeText(this, String.format(java.util.Locale.US, "CORE MULTIPLIER: %.2fx", totalMult), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSongs(); // Refresh scores when returning to this screen
    }

    private void loadSongs() {
        databaseService.getSongsSnapshot(new DatabaseService.DatabaseCallback<List<DataSnapshot>>() {
            @Override
            public void onCompleted(List<DataSnapshot> snapshots) {
                songSnapshots = snapshots;
                List<SongData> songs = new ArrayList<>();
                for (DataSnapshot snap : snapshots) {
                    SongData s = snap.getValue(SongData.class);
                    if (s != null) songs.add(s);
                }
                adapter.updateList(songs);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(SongSelectionActivity.this, "Failed to load songs.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
