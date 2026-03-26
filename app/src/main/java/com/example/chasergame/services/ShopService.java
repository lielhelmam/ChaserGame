package com.example.chasergame.services;

import com.example.chasergame.models.Skin;
import com.example.chasergame.models.User;

import org.jetbrains.annotations.NotNull;

public class ShopService {
    private static final int GIFT_AMOUNT = 100000;
    private final IUserRepository userRepository;

    public ShopService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void claimGift(@NotNull User user, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        if (!user.isGiftClaimed()) {
            user.setPoints(user.getPoints() + GIFT_AMOUNT);
            user.setGiftClaimed(true);
            userRepository.updateUser(user, callback);
        } else {
            callback.onFailed(new Exception("Gift already claimed"));
        }
    }

    public void buySkin(@NotNull User user, @NotNull Skin skin, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        if (user.getPoints() >= skin.price) {
            user.setPoints(user.getPoints() - skin.price);
            user.getOwnedSkins().add(skin.id);
            userRepository.updateUser(user, callback);
        } else {
            callback.onFailed(new Exception("Not enough points"));
        }
    }

    public void equipSkin(@NotNull User user, @NotNull String skinId, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        user.setEquippedSkin(skinId);
        userRepository.updateUser(user, callback);
    }

    public void updateCustomSkin(@NotNull User user, int circle, int target, int bg, String effect, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        user.setCustomCircleColor(circle);
        user.setCustomTargetColor(target);
        user.setCustomBackgroundColor(bg);
        user.setCustomEffectType(effect);
        userRepository.updateUser(user, callback);
    }
}
