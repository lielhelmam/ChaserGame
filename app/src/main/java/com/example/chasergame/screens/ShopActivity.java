package com.example.chasergame.screens;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.SkinManager;

import java.util.List;

public class ShopActivity extends BaseActivity {

    private static final int[] PRESET_COLORS = {
            Color.WHITE, Color.BLACK, Color.parseColor("#B71C1C"), Color.GREEN,
            Color.BLUE, Color.parseColor("#FBC02D"), Color.CYAN, Color.MAGENTA,
            Color.parseColor("#FFA500"), Color.parseColor("#800080"),
            Color.parseColor("#FFD700"), Color.GRAY
    };

    private TextView tvPoints;
    private RecyclerView rvSkins;
    private View btnClaimGift;
    private SkinsAdapter adapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        tvPoints = findViewById(R.id.tv_user_points_shop);
        rvSkins = findViewById(R.id.rv_skins);
        btnClaimGift = findViewById(R.id.btn_claim_gift);
        findViewById(R.id.btn_back_from_shop).setOnClickListener(v -> finish());

        currentUser = authService.getCurrentUser();
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
        btnClaimGift.setVisibility(currentUser.isGiftClaimed() ? View.GONE : View.VISIBLE);
    }

    private void claimGift() {
        shopService.claimGift(currentUser, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                authService.syncUser(currentUser);
                updateUI();
                checkGiftStatus();
                Toast.makeText(ShopActivity.this, "Gift claimed!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ShopActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        tvPoints.setText("Points: " + currentUser.getPoints());
    }

    private void setupRecyclerView() {
        List<Skin> skins = SkinManager.getSkins();
        adapter = new SkinsAdapter(skins, currentUser, new SkinsAdapter.OnSkinActionListener() {
            @Override
            public void onBuy(Skin skin) {
                shopService.buySkin(currentUser, skin, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void unused) {
                        authService.syncUser(currentUser);
                        updateUI();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(ShopActivity.this, "Purchased " + skin.name, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(ShopActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onEquip(Skin skin) {
                shopService.equipSkin(currentUser, skin.id, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void unused) {
                        authService.syncUser(currentUser);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(ShopActivity.this, "Equipped " + skin.name, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(Exception e) {
                    }
                });
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
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_edit_custom_skin, null);
        Spinner spCircle = v.findViewById(R.id.sp_custom_circle_color);
        Spinner spTarget = v.findViewById(R.id.sp_custom_target_color);
        Spinner spBg = v.findViewById(R.id.sp_custom_bg_color);
        Spinner spEffect = v.findViewById(R.id.sp_custom_effect);

        spCircle.setSelection(getColorIndex(currentUser.getCustomCircleColor()));
        spTarget.setSelection(getColorIndex(currentUser.getCustomTargetColor()));
        spBg.setSelection(getColorIndex(currentUser.getCustomBackgroundColor()));

        if (currentUser.getCustomEffectType() != null) {
            ArrayAdapter<CharSequence> effectAdapter = (ArrayAdapter<CharSequence>) spEffect.getAdapter();
            int pos = effectAdapter.getPosition(currentUser.getCustomEffectType());
            if (pos >= 0) spEffect.setSelection(pos);
        }

        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton("Save", (dialog, which) -> {
                    int circle = PRESET_COLORS[spCircle.getSelectedItemPosition()];
                    int target = PRESET_COLORS[spTarget.getSelectedItemPosition()];
                    int bg = PRESET_COLORS[spBg.getSelectedItemPosition()];
                    String effect = spEffect.getSelectedItem().toString();

                    shopService.updateCustomSkin(currentUser, circle, target, bg, effect, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void unused) {
                            authService.syncUser(currentUser);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(ShopActivity.this, "Custom skin updated!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailed(Exception e) {
                        }
                    });
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
}
