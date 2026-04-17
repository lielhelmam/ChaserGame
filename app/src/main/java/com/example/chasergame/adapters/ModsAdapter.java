package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;

import java.util.Set;

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ViewHolder> {

    private final String[] names;
    private final String[] descs;
    private final String[] keys;
    private final boolean[] selected;
    private final Set<String> activeMods;

    public ModsAdapter(String[] names, String[] descs, String[] keys, boolean[] selected, Set<String> activeMods) {
        this.names = names;
        this.descs = descs;
        this.keys = keys;
        this.selected = selected;
        this.activeMods = activeMods;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mod, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvName.setText(names[position]);
        holder.tvDesc.setText(descs[position]);
        holder.checkBox.setChecked(selected[position]);

        holder.itemView.setOnClickListener(v -> {
            selected[position] = !selected[position];
            holder.checkBox.setChecked(selected[position]);
            updateActiveMods(position, selected[position]);
        });

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selected[position] = isChecked;
            updateActiveMods(position, isChecked);
        });
    }

    private void updateActiveMods(int position, boolean isChecked) {
        if (isChecked) activeMods.add(keys[position]);
        else activeMods.remove(keys[position]);
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_mod_name);
            tvDesc = itemView.findViewById(R.id.tv_mod_desc);
            checkBox = itemView.findViewById(R.id.cb_mod_selected);
        }
    }
}