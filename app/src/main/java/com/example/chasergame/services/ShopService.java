package com.example.chasergame.services;

import com.example.chasergame.models.Skin;
import com.example.chasergame.models.User;

import org.jetbrains.annotations.NotNull;

public class ShopService {
    private static final int GIFT_AMOUNT = 100000;
    private final IUserRepository userRepository;

    /**
     * Constructor for ShopService.
     *
     * @param userRepository The repository to handle user data updates.
     */
    public ShopService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Allows a user to claim a one-time gift of points.
     * Checks if the gift was already claimed before adding points.
     *
     * @param user     The User object claiming the gift.
     * @param callback Callback to handle the result of the operation.
     */
    public void claimGift(@NotNull User user, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        if (!user.isGiftClaimed()) {
            user.setPoints(user.getPoints() + GIFT_AMOUNT);
            user.setGiftClaimed(true);
            userRepository.updateUser(user, callback);
        } else {
            callback.onFailed(new Exception("Gift already claimed"));
        }
    }

    /**
     * Handles the purchase of a skin by a user.
     * Deducts points and adds the skin ID to the user's owned skins list if they have enough points.
     *
     * @param user     The User object making the purchase.
     * @param skin     The Skin object to be purchased.
     * @param callback Callback to handle the result of the purchase attempt.
     */
    public void buySkin(@NotNull User user, @NotNull Skin skin, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        if (user.getPoints() >= skin.price) {
            user.setPoints(user.getPoints() - skin.price);
            user.getOwnedSkins().add(skin.id);
            userRepository.updateUser(user, callback);
        } else {
            callback.onFailed(new Exception("Not enough points"));
        }
    }

    /**
     * Sets a specific skin as the user's currently equipped skin.
     *
     * @param user     The User object updating their equipment.
     * @param skinId   The ID of the skin to equip.
     * @param callback Callback to handle the result of the operation.
     */
    public void equipSkin(@NotNull User user, @NotNull String skinId, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        user.setEquippedSkin(skinId);
        userRepository.updateUser(user, callback);
    }

    /**
     * Updates the custom skin colors and effects for a user.
     *
     * @param user     The User object to update.
     * @param circle   The color for the player's circle.
     * @param target   The color for the target/background element.
     * @param bg       The background color.
     * @param effect   The type of visual effect (e.g., "glow", "particles").
     * @param callback Callback to handle the result of the update.
     */
    public void updateCustomSkin(@NotNull User user, int circle, int target, int bg, String effect, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        user.setCustomCircleColor(circle);
        user.setCustomTargetColor(target);
        user.setCustomBackgroundColor(bg);
        user.setCustomEffectType(effect);
        userRepository.updateUser(user, callback);
    }
}
