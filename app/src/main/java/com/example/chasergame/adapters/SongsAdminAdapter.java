package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.SongData;

import java.util.ArrayList;
import java.util.List;

public class SongsAdminAdapter extends RecyclerView.Adapter<SongsAdminAdapter.VH> {

    private final Listener listener;
    private final List<Item> visible = new ArrayList<>();
    private final List<Item> all = new ArrayList<>();

    public SongsAdminAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        all.clear();
        all.addAll(items);
        visible.clear();
        visible.addAll(items);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        visible.clear();

        if (q.isEmpty()) {
            visible.addAll(all);
        } else {
            for (Item item : all) {
                if (item.value.getName().toLowerCase().contains(q)) {
                    visible.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_admin, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item item = visible.get(position);
        SongData song = item.value;

        h.tvName.setText(song.getName());
        h.tvDifficulty.setText(song.getDifficulty());
        h.tvRes.setText("Resource: " + song.getResName());
        h.tvTarget.setText("Target Score: " + song.getTargetScore());

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(item.key, song);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(item.key);
        });
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    public interface Listener {
        void onEditClicked(String key, SongData song);
        void onDeleteClicked(String key);
    }

    public static class Item {
        public final String key;
        public final SongData value;

        public Item(String key, SongData value) {
            this.key = key;
            this.value = value;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvDifficulty, tvRes, tvTarget;
        final Button btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_admin_song_name);
            tvDifficulty = itemView.findViewById(R.id.tv_admin_song_difficulty);
            tvRes = itemView.findViewById(R.id.tv_admin_song_res);
            tvTarget = itemView.findViewById(R.id.tv_admin_target_score);
            btnEdit = itemView.findViewById(R.id.btn_song_edit);
            btnDelete = itemView.findViewById(R.id.btn_song_delete);
        }
    }
}
