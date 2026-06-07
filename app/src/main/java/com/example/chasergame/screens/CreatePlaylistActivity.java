package com.example.chasergame.screens;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SelectableSongsAdapter;
import com.example.chasergame.models.Playlist;
import com.example.chasergame.services.DatabaseService;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CreatePlaylistActivity extends BaseActivity {

    private EditText etName;
    private SelectableSongsAdapter adapter;
    private Playlist existingPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_playlist);

        etName = findViewById(R.id.et_playlist_name);
        SearchView searchView = findViewById(R.id.sv_selectable_songs);
        RecyclerView rv = findViewById(R.id.rv_selectable_songs);
        rv.setLayoutManager(new LinearLayoutManager(this));

        existingPlaylist = (Playlist) getIntent().getSerializableExtra("PLAYLIST_TO_EDIT");

        databaseService.getSongsSnapshot(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<DataSnapshot> snapshots) {
                adapter = new SelectableSongsAdapter(snapshots);
                if (existingPlaylist != null) {
                    etName.setText(existingPlaylist.getName());
                    adapter.setSelectedIds(existingPlaylist.getSongIds());
                }
                rv.setAdapter(adapter);

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
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(CreatePlaylistActivity.this, "Failed to load songs", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_save_playlist).setOnClickListener(v -> savePlaylist());
    }

    private void savePlaylist() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }

        List<String> selectedIds = adapter.getSelectedIds();
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Select at least one song", Toast.LENGTH_SHORT).show();
            return;
        }

        Playlist p = existingPlaylist != null ? existingPlaylist : new Playlist(null, name, selectedIds);
        p.setName(name);
        p.setSongIds(selectedIds);

        databaseService.createPlaylist(authService.getCurrentUser().getId(), p, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(Void object) {
                String msg = existingPlaylist != null ? "Playlist updated!" : "Playlist created!";
                Toast.makeText(CreatePlaylistActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(CreatePlaylistActivity.this, "Failed to create playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
