package com.example.chasergame.screens;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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
    private SkinsAdapter adapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        tvPoints = findViewById(R.id.tv_user_points_shop);
        rvSkins = findViewById(R.id.rv_skins);
        findViewById(R.id.btn_back_from_shop).setOnClickListener(v -> finish());

        currentUser = SharedPreferencesUtil.getUser(this);
        if (currentUser == null) {
            finish();
            return;
        }

        updateUI();
        setupRecyclerView();
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
        });
        rvSkins.setLayoutManager(new LinearLayoutManager(this));
        rvSkins.setAdapter(adapter);
    }

    private void saveUser() {
        SharedPreferencesUtil.saveUser(this, currentUser);
        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getId())
                .setValue(currentUser);
    }
}
