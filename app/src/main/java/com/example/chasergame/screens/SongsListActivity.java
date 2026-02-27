package com.example.chasergame.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SongsAdminAdapter;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.SongData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SongsListActivity extends BaseActivity {

    private SongsAdminAdapter adapter;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs_list);

        mDatabase = FirebaseDatabase.getInstance().getReference("rhythm_songs");

        Toolbar toolbar = findViewById(R.id.toolbarSongs);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_songs_list);
        SearchView searchView = findViewById(R.id.searchViewSongs);

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

        loadSongs();
    }

    private void loadSongs() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SongsAdminAdapter.Item> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    SongData song = child.getValue(SongData.class);
                    if (key != null && song != null) {
                        items.add(new SongsAdminAdapter.Item(key, song));
                    }
                }
                adapter.setItems(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SongsListActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(String key) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Song")
                .setMessage("Are you sure you want to delete this song?")
                .setPositiveButton("Delete", (d, w) -> {
                    mDatabase.child(key).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(SongsListActivity.this, "Song deleted.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(SongsListActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
        etBeatmap.setText(notesToString(song.getNotes()));

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
                    List<Note> notes = parseBeatmap(beatmapStr);

                    if (notes.isEmpty()) {
                        Toast.makeText(this, "Invalid beatmap format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SongData updatedSong = new SongData(name, difficulty, 0, notes, targetScore);
                    updatedSong.setResName(resName);

                    mDatabase.child(key).setValue(updatedSong)
                            .addOnSuccessListener(aVoid -> Toast.makeText(SongsListActivity.this, "Song updated.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(SongsListActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<Note> parseBeatmap(String beatmapStr) {
        List<Note> notes = new ArrayList<>();
        try {
            String[] entries = beatmapStr.split(";");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    long time = Long.parseLong(parts[0].trim());
                    int lane = Integer.parseInt(parts[1].trim());
                    notes.add(new Note(time, lane));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notes;
    }

    private String notesToString(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            sb.append(note.getTimestamp()).append(",").append(note.getLane());
            if (i < notes.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }
}
