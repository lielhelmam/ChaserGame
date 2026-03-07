package com.example.chasergame.screens;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.SkinsAdapter;
import com.example.chasergame.models.Skin;
import com.example.chasergame.models.User;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.example.chasergame.utils.SkinManager;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ShopActivity extends BaseActivity {

    private TextView tvPoints;
    private RecyclerView rvSkins;
    private Button btnClaimGift;
    private SkinsAdapter adapter;
    private User currentUser;

    private static final int GIFT_AMOUNT = 100000;

    private static final int[] PRESET_COLORS = {
            Color.WHITE,   // White
            Color.BLACK,   // Black
            Color.parseColor("#B71C1C"), // Dark Red
            Color.GREEN,   // Green
            Color.BLUE,    // Blue
            Color.parseColor("#FBC02D"), // Dark Yellow
            Color.CYAN,    // Cyan
            Color.MAGENTA, // Magenta
            Color.parseColor("#FFA500"), // Orange
            Color.parseColor("#800080"), // Purple
            Color.parseColor("#FFD700"), // Gold
            Color.GRAY     // Gray
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        tvPoints = findViewById(R.id.tv_user_points_shop);
        rvSkins = findViewById(R.id.rv_skins);
        btnClaimGift = findViewById(R.id.btn_claim_gift);
        findViewById(R.id.btn_back_from_shop).setOnClickListener(v -> finish());

        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        updateUI();
        setupRecyclerView();
        checkGiftStatus();
        
        btnClaimGift.setOnClickListener(v -> claimGift());
    }

    private void checkGiftStatus() {
        if (!currentUser.isGiftClaimed()) {
            btnClaimGift.setVisibility(View.VISIBLE);
        } else {
            btnClaimGift.setVisibility(View.GONE);
        }
    }

    private void claimGift() {
        if (!currentUser.isGiftClaimed()) {
            currentUser.setPoints(currentUser.getPoints() + GIFT_AMOUNT);
            currentUser.setGiftClaimed(true);
            saveUser();
            updateUI();
            btnClaimGift.setVisibility(View.GONE);
            Toast.makeText(this, "You received 100,000 points! Enjoy!", Toast.LENGTH_LONG).show();
        }
    }

    private void updateUI() {
        tvPoints.setText("Points: " + currentUser.getPoints());
    }

    private void setupRecyclerView() {
        List<Skin> skins = SkinManager.getSkins();
        adapter = new SkinsAdapter(skins, currentUser, new SkinsAdapter.OnSkinActionListener() {
            @Override
            public void onBuy(Skin skin) {
                if (currentUser.getPoints() >= skin.price) {
                    currentUser.setPoints(currentUser.getPoints() - skin.price);
                    currentUser.getOwnedSkins().add(skin.id);
                    saveUser();
                    updateUI();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ShopActivity.this, "Purchased " + skin.name, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onEquip(Skin skin) {
                currentUser.setEquippedSkin(skin.id);
                saveUser();
                adapter.notifyDataSetChanged();
                Toast.makeText(ShopActivity.this, "Equipped " + skin.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditCustom() {
                showEditCustomSkinDialog();
            }
        });
        rvSkins.setLayoutManager(new LinearLayoutManager(this));
        rvSkins.setAdapter(adapter);
    }

    private void showEditCustomSkinDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_custom_skin, null);
        Spinner spCircle = dialogView.findViewById(R.id.sp_custom_circle_color);
        Spinner spTarget = dialogView.findViewById(R.id.sp_custom_target_color);
        Spinner spBg = dialogView.findViewById(R.id.sp_custom_bg_color);
        Spinner spEffect = dialogView.findViewById(R.id.sp_custom_effect);

        spCircle.setSelection(getColorIndex(currentUser.getCustomCircleColor()));
        spTarget.setSelection(getColorIndex(currentUser.getCustomTargetColor()));
        spBg.setSelection(getColorIndex(currentUser.getCustomBackgroundColor()));
        
        ArrayAdapter<CharSequence> effectAdapter = (ArrayAdapter<CharSequence>) spEffect.getAdapter();
        if (currentUser.getCustomEffectType() != null) {
            int pos = effectAdapter.getPosition(currentUser.getCustomEffectType());
            if (pos >= 0) spEffect.setSelection(pos);
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    currentUser.setCustomCircleColor(PRESET_COLORS[spCircle.getSelectedItemPosition()]);
                    currentUser.setCustomTargetColor(PRESET_COLORS[spTarget.getSelectedItemPosition()]);
                    currentUser.setCustomBackgroundColor(PRESET_COLORS[spBg.getSelectedItemPosition()]);
                    currentUser.setCustomEffectType(spEffect.getSelectedItem().toString());
                    
                    saveUser();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Custom skin updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int getColorIndex(int color) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == color) return i;
        }
        return 0;
    }

    private void saveUser() {
        SharedPreferencesUtil.saveUser(this, currentUser);
        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getId())
                .setValue(currentUser);
    }
}
