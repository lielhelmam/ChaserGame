package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.PlaylistsAdapter;
import com.example.chasergame.models.Playlist;
import com.example.chasergame.services.DatabaseService;

import java.util.List;

public class MyPlaylistsActivity extends BaseActivity {

    private RecyclerView rvPlaylists;
    private PlaylistsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_playlists);

        rvPlaylists = findViewById(R.id.rv_my_playlists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));

        loadPlaylists();

        findViewById(R.id.btn_create_playlist).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePlaylistActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_back_to_selection).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void loadPlaylists() {
        databaseService.getUserPlaylists(authService.getCurrentUser().getId(), new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<Playlist> playlists) {
                adapter = new PlaylistsAdapter(playlists, new PlaylistsAdapter.OnPlaylistClickListener() {
                    @Override
                    public void onPlaylistClick(Playlist playlist) {
                        Intent intent = new Intent(MyPlaylistsActivity.this, PlaylistDetailsActivity.class);
                        intent.putExtra("PLAYLIST", playlist);
                        startActivity(intent);
                    }

                    @Override
                    public void onEditClick(Playlist playlist) {
                        Intent intent = new Intent(MyPlaylistsActivity.this, CreatePlaylistActivity.class);
                        intent.putExtra("PLAYLIST_TO_EDIT", playlist);
                        startActivity(intent);
                    }

                    @Override
                    public void onDeleteClick(Playlist playlist) {
                        new AlertDialog.Builder(MyPlaylistsActivity.this)
                                .setTitle("Delete Playlist")
                                .setMessage("Are you sure?")
                                .setPositiveButton("Yes", (d, i) -> {
                                    databaseService.deletePlaylist(authService.getCurrentUser().getId(), playlist.getId(), new DatabaseService.DatabaseCallback<>() {
                                        @Override
                                        public void onCompleted(Void object) {
                                            loadPlaylists();
                                        }

                                        @Override
                                        public void onFailed(Exception e) {
                                            Toast.makeText(MyPlaylistsActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton("No", null)
                                .show();
                    }
                });
                rvPlaylists.setAdapter(adapter);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(MyPlaylistsActivity.this, "Failed to load playlists", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
