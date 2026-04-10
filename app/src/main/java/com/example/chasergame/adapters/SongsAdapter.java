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
import com.example.chasergame.services.AuthService;
import com.example.chasergame.models.User;

import java.util.ArrayList;
import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {

    private List<SongData> songList;
    private List<SongData> songListFull; // Original full list for filtering
    private OnSongClickListener listener;
    private AuthService authService;

    public SongsAdapter(List<SongData> songList, AuthService authService, OnSongClickListener listener) {
        this.songList = songList;
        this.songListFull = new ArrayList<>(songList);
        this.authService = authService;
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
        holder.tvDifficulty.setText(song.getDifficulty());
        holder.tvBpm.setText(song.getBpm() + " BPM");
        
        // World Record (Global Top)
        String wrName = song.getTopPlayerName();
        if (wrName == null || wrName.isEmpty()) wrName = "None";
        holder.tvTopScore.setText("WR: " + song.getTopScore() + " (" + wrName + ")");

        // Personal Best & Rank
        if (authService != null) {
            User currentUser = authService.getCurrentUser();
            if (currentUser != null && currentUser.songHighScores != null && currentUser.songHighScores.containsKey(song.getName())) {
                int pbScore = currentUser.songHighScores.get(song.getName());
                String pbRank = currentUser.songRanks != null ? currentUser.songRanks.getOrDefault(song.getName(), "-") : "-";
                
                holder.tvTopRank.setText(pbRank);
                holder.tvTopAccuracy.setText("PB: " + pbScore);
            } else {
                holder.tvTopRank.setText("-");
                holder.tvTopAccuracy.setText("PB: 0");
            }
        }

        holder.itemLayout.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(song);
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
        TextView tvName, tvDifficulty, tvBpm, tvTopScore, tvTopAccuracy, tvTopRank;
        LinearLayout itemLayout;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_song_name);
            tvDifficulty = itemView.findViewById(R.id.tv_song_difficulty);
            tvBpm = itemView.findViewById(R.id.tv_song_bpm);
            tvTopScore = itemView.findViewById(R.id.tv_top_score);
            tvTopAccuracy = itemView.findViewById(R.id.tv_top_accuracy);
            tvTopRank = itemView.findViewById(R.id.tv_top_rank);
            itemLayout = itemView.findViewById(R.id.song_item_layout);
        }
    }
}
