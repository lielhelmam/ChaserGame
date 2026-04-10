package com.example.chasergame.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.SongData;

import java.util.ArrayList;
import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {

    private List<SongData> songList;
    private List<SongData> songListFull; // Original full list for filtering
    private OnSongClickListener listener;

    public SongsAdapter(List<SongData> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.songListFull = new ArrayList<>(songList);
        this.listener = listener;
    }

    public void updateList(List<SongData> newList) {
        this.songList = newList;
        this.songListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        List<SongData> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(songListFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (SongData song : songListFull) {
                if (song.getName().toLowerCase().contains(filterPattern) ||
                        song.getDifficulty().toLowerCase().contains(filterPattern)) {
                    filteredList.add(song);
                }
            }
        }
        this.songList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SongData song = songList.get(position);
        holder.tvName.setText(song.getName());
        holder.tvDifficulty.setText("Difficulty: " + song.getDifficulty() + " (HP Miss: -" + song.getHpDrain() + ")");
        holder.tvBpm.setText(song.getBpm() + " BPM");

        holder.itemLayout.setOnClickListener(v -> {
            Log.d("SongsAdapter", "Item clicked via layout: " + song.getName());
            if (listener != null) {
                listener.onSongClick(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public interface OnSongClickListener {
        void onSongClick(SongData song);
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDifficulty, tvBpm;
        LinearLayout itemLayout;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_song_name);
            tvDifficulty = itemView.findViewById(R.id.tv_song_difficulty);
            tvBpm = itemView.findViewById(R.id.tv_song_bpm);
            itemLayout = itemView.findViewById(R.id.song_item_layout);
        }
    }
}
