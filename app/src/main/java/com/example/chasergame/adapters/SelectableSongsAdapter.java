package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.SongData;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableSongsAdapter extends RecyclerView.Adapter<SelectableSongsAdapter.VH> {

    private final List<DataSnapshot> snapshots;
    private final Set<String> selectedIds = new HashSet<>();

    public SelectableSongsAdapter(List<DataSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selectable_song, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DataSnapshot snap = snapshots.get(position);
        SongData song = snap.getValue(SongData.class);
        String id = snap.getKey();

        if (song != null) {
            holder.tvName.setText(song.getName());
            holder.tvDiff.setText(song.getDifficulty());
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedIds.contains(id));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedIds.add(id);
                else selectedIds.remove(id);
            });
        }
    }

    @Override
    public int getItemCount() {
        return snapshots.size();
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public void setSelectedIds(List<String> ids) {
        if (ids != null) {
            selectedIds.clear();
            selectedIds.addAll(ids);
            notifyDataSetChanged();
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDiff;
        CheckBox checkBox;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_song_name);
            tvDiff = itemView.findViewById(R.id.tv_song_difficulty);
            checkBox = itemView.findViewById(R.id.cb_select_song);
        }
    }
}
