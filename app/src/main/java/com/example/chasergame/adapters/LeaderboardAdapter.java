package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.LeaderboardEntry;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private final List<LeaderboardEntry> leaderboardEntries;

    public LeaderboardAdapter(List<LeaderboardEntry> leaderboardEntries) {
        this.leaderboardEntries = leaderboardEntries;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.leaderboard_item, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardEntry entry = leaderboardEntries.get(position);
        holder.rank.setText(String.valueOf(entry.getRank()));
        holder.username.setText(entry.getUsername());
        holder.wins.setText(String.valueOf(entry.getScore()));
    }

    @Override
    public int getItemCount() {
        return leaderboardEntries.size();
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        final TextView rank;
        final TextView username;
        final TextView wins;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.tv_rank);
            username = itemView.findViewById(R.id.tv_username);
            wins = itemView.findViewById(R.id.tv_wins);
        }
    }
}