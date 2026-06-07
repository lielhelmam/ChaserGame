package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.Playlist;

import java.util.List;

public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.VH> {

    private final List<Playlist> playlists;
    private final OnPlaylistClickListener listener;

    public PlaylistsAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Playlist p = playlists.get(position);
        holder.tvName.setText(p.getName());
        holder.tvCount.setText(p.getSongIds().size() + " songs");
        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(p));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(p));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(p));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);

        void onEditClick(Playlist playlist);

        void onDeleteClick(Playlist playlist);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        View btnEdit, btnDelete;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_playlist_name);
            tvCount = itemView.findViewById(R.id.tv_playlist_count);
            btnEdit = itemView.findViewById(R.id.btn_edit_playlist);
            btnDelete = itemView.findViewById(R.id.btn_delete_playlist);
        }
    }
}
