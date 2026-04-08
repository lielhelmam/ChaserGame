package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.Skin;
import com.example.chasergame.models.User;

import java.util.List;

public class SkinsAdapter extends RecyclerView.Adapter<SkinsAdapter.ViewHolder> {

    private List<Skin> skins;
    private User currentUser;
    private final OnSkinActionListener listener;

    public SkinsAdapter(List<Skin> skins, User user, OnSkinActionListener listener) {
        this.skins = skins;
        this.currentUser = user;
        this.listener = listener;
    }

    public void updateUser(User user) {
        this.currentUser = user;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_skin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Skin skin = skins.get(position);
        holder.tvName.setText(skin.name);

        // Handle preview and info for custom skin
        if ("custom".equals(skin.id)) {
            holder.tvEffect.setText("Effect: " + currentUser.getCustomEffectType());
            holder.previewBg.setBackgroundColor(currentUser.getCustomBackgroundColor());
            holder.previewCircle.getBackground().setTint(currentUser.getCustomCircleColor());
            holder.previewTarget.getBackground().setTint(currentUser.getCustomTargetColor());
            holder.btnEdit.setVisibility(currentUser.getOwnedSkins().contains("custom") ? View.VISIBLE : View.GONE);
            holder.btnEdit.setOnClickListener(v -> listener.onEditCustom());
        } else {
            holder.tvEffect.setText("Effect: " + skin.effectType);
            holder.previewBg.setBackgroundColor(skin.backgroundColor);
            holder.previewCircle.getBackground().setTint(skin.circleColor);
            holder.previewTarget.getBackground().setTint(skin.targetColor);
            holder.btnEdit.setVisibility(View.GONE);
        }

        boolean isOwned = currentUser.getOwnedSkins().contains(skin.id);
        boolean isEquipped = skin.id.equals(currentUser.getEquippedSkin());

        if (isEquipped) {
            holder.btnAction.setText("Equipped");
            holder.btnAction.setEnabled(false);
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
        } else if (isOwned) {
            holder.btnAction.setText("Equip");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3)); // Blue
            holder.btnAction.setOnClickListener(v -> listener.onEquip(skin));
        } else {
            holder.btnAction.setText(skin.price + " pts");
            boolean canAfford = currentUser.getPoints() >= skin.price;
            holder.btnAction.setEnabled(canAfford);

            // Set color based on affordability: Blue if can buy, Red if cannot
            int color = canAfford ? 0xFF2196F3 : 0xFFB71C1C;
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

            holder.btnAction.setOnClickListener(v -> listener.onBuy(skin));
        }
    }

    @Override
    public int getItemCount() {
        return skins.size();
    }

    public interface OnSkinActionListener {
        void onBuy(Skin skin);

        void onEquip(Skin skin);

        void onEditCustom();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEffect;
        View previewBg, previewCircle, previewTarget;
        Button btnAction, btnEdit;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_skin_name);
            tvEffect = v.findViewById(R.id.tv_skin_effect);
            previewBg = v.findViewById(R.id.view_preview_bg);
            previewCircle = v.findViewById(R.id.view_preview_circle);
            previewTarget = v.findViewById(R.id.view_preview_target);
            btnAction = v.findViewById(R.id.btn_skin_action);
            btnEdit = v.findViewById(R.id.btn_edit_custom_skin);
        }
    }
}
