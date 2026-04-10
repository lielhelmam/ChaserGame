package com.example.chasergame.services;

import com.example.chasergame.adapters.SongsAdminAdapter;
import com.example.chasergame.models.SongData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ISongRepository {
    void getSongList(@NotNull DatabaseService.DatabaseCallback<List<SongData>> callback);

    void getSongListWithKeys(@NotNull DatabaseService.DatabaseCallback<List<SongsAdminAdapter.Item>> callback);

    void getSongById(@NotNull String songId, @NotNull DatabaseService.DatabaseCallback<SongData> callback);

    void addSong(@NotNull SongData song, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void deleteSong(@NotNull String songId, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void updateSong(@NotNull String songId, @NotNull SongData song, @Nullable DatabaseService.DatabaseCallback<Void> callback);
}
