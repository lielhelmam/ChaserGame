package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        
        // Customizing the title with a techy feel
        builder.setTitle("SYSTEM DISRUPTORS");
        
        builder.setMultiChoiceItems(modNames, selectedMods, (dialog, which, isChecked) -> {
            selectedMods[which] = isChecked;
            if (isChecked) activeMods.add(modKeys[which]);
            else activeMods.remove(modKeys[which]);
        });

        builder.setPositiveButton("ACTIVATE", (dialog, which) -> {
            double totalMult = 1.0;
            for (int i = 0; i < selectedMods.length; i++) {
                if (selectedMods[i]) totalMult *= modMultipliers[i];
            }
            if (totalMult > 1.0) {
                Toast.makeText(this, String.format(java.util.Locale.US, "SYSTEM OVERCLOCK: %.2fx", totalMult), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("CLEAR ALL", (dialog, which) -> {
            for (int i = 0; i < selectedMods.length; i++) selectedMods[i] = false;
            activeMods.clear();
            Toast.makeText(this, "Systems Restored", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            // Making it look sleek and dark
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.target_cube_bg);
        }
        dialog.show();
        
        // Style the buttons after show()
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#00FFFF"));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(android.graphics.Color.GRAY);
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
