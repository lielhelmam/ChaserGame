package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.SongData;

import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {

    private List<SongData> songList;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(SongData song);
    }

    public SongsAdapter(List<SongData> songList, OnSongClickListener listener) {
        this.songList = songList;
        this.listener = listener;
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
        holder.tvDifficulty.setText("Difficulty: " + song.getDifficulty());
        holder.tvTargetScore.setText("Target: " + song.getTargetScore());

        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDifficulty, tvTargetScore;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_song_name);
            tvDifficulty = itemView.findViewById(R.id.tv_song_difficulty);
            tvTargetScore = itemView.findViewById(R.id.tv_target_score);
        }
    }
}
