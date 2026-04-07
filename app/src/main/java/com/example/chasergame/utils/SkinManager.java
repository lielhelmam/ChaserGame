package com.example.chasergame.utils;

import android.graphics.Color;

import com.example.chasergame.models.Skin;
import com.example.chasergame.models.User;

import java.util.ArrayList;
import java.util.List;

public class SkinManager {
    private static List<Skin> skins;

    /**
     * Retrieves the list of all available skins in the game.
     * Initializes the list with a default set of skins if it hasn't been created yet.
     *
     * @return A list of Skin objects.
     */
    public static List<Skin> getSkins() {
        if (skins == null) {
            skins = new ArrayList<>();
            // Default Skin
            skins.add(new Skin("default", "Classic Red", 0, Color.parseColor("#FFFFFF"), Color.parseColor("#bd5757"), Color.parseColor("#1A1A1A"), "none"));

            // Neon Skin
            skins.add(new Skin("neon", "Neon Night", 100000, Color.parseColor("#00FBFF"), Color.parseColor("#00FBFF"), Color.parseColor("#000000"), "glow"));

            // Fire Skin
            skins.add(new Skin("fire", "Hellfire", 100000, Color.parseColor("#FF5722"), Color.parseColor("#FFC107"), Color.parseColor("#210000"), "particles"));

            // Ocean Skin
            skins.add(new Skin("ocean", "Deep Ocean", 100000, Color.parseColor("#00BCD4"), Color.parseColor("#03A9F4"), Color.parseColor("#001219"), "bubbles"));

            // Golden Skin
            skins.add(new Skin("gold", "Midas Touch", 100000, Color.parseColor("#FFD700"), Color.parseColor("#DAA520"), Color.parseColor("#1C1C1C"), "sparkle"));

            // Cyberpunk Skin
            skins.add(new Skin("cyberpunk", "Cyberpunk", 100000, Color.parseColor("#FF00FF"), Color.parseColor("#9D00FF"), Color.parseColor("#0D0221"), "glow"));

            // Emerald Skin
            skins.add(new Skin("emerald", "Emerald Forest", 100000, Color.parseColor("#2ECC71"), Color.parseColor("#F1C40F"), Color.parseColor("#0B2010"), "particles"));

            // Abyssal Void
            skins.add(new Skin("void", "Abyssal Void", 100000, Color.parseColor("#00E5FF"), Color.parseColor("#311B92"), Color.parseColor("#000000"), "glow"));

            // Sunset Drive
            skins.add(new Skin("sunset", "Sunset Drive", 100000, Color.parseColor("#FF7043"), Color.parseColor("#EC407A"), Color.parseColor("#260126"), "glow"));

            // Forest Spirit
            skins.add(new Skin("forest", "Forest Spirit", 100000, Color.parseColor("#8BC34A"), Color.parseColor("#689F38"), Color.parseColor("#1B1B0E"), "particles"));

            // Frozen Glaze
            skins.add(new Skin("frozen", "Frozen Glaze", 100000, Color.parseColor("#B3E5FC"), Color.parseColor("#FFFFFF"), Color.parseColor("#01579B"), "bubbles"));

            // Toxic Waste
            skins.add(new Skin("toxic", "Toxic Waste", 100000, Color.parseColor("#CCFF00"), Color.parseColor("#CCFF00"), Color.parseColor("#1A1A1A"), "glow"));

            // --- MORE UNIQUE SKINS ---

            // Nebula Drift
            skins.add(new Skin("nebula", "Nebula Drift", 100000, Color.parseColor("#E040FB"), Color.parseColor("#7C4DFF"), Color.parseColor("#08001F"), "glow"));

            // Ghostly Whisper
            skins.add(new Skin("ghost", "Ghostly Whisper", 100000, Color.parseColor("#CFD8DC"), Color.parseColor("#80DEEA"), Color.parseColor("#263238"), "particles"));

            // Retro Wave
            skins.add(new Skin("retro", "Retro Wave", 100000, Color.parseColor("#FF007F"), Color.parseColor("#00FFFF"), Color.parseColor("#000022"), "glow"));

            // Blood Moon
            skins.add(new Skin("bloodmoon", "Blood Moon", 100000, Color.parseColor("#D50000"), Color.parseColor("#212121"), Color.parseColor("#000000"), "particles"));

            // Electric Storm
            skins.add(new Skin("electric", "Electric Storm", 100000, Color.parseColor("#FFFF00"), Color.parseColor("#0D47A1"), Color.parseColor("#000000"), "glow"));

            // Custom Skin Placeholder
            skins.add(new Skin("custom", "Custom Creation", 500000, Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), Color.parseColor("#333333"), "none"));
        }
        return skins;
    }

    /**
     * Finds and returns a skin by its unique identifier.
     *
     * @param id The ID of the skin to find.
     * @return The Skin object matching the ID, or the default skin if not found.
     */
    public static Skin getSkinById(String id) {
        for (Skin skin : getSkins()) {
            if (skin.id.equals(id)) return skin;
        }
        return getSkins().get(0);
    }

    /**
     * Creates a custom skin based on the user's stored custom color preferences.
     *
     * @param user The User object containing custom skin data.
     * @return A Skin object with the user's custom settings.
     */
    public static Skin getCustomSkinForUser(User user) {
        if (user == null) return getSkinById("default");
        return new Skin("custom", "Custom Creation", 500000,
                user.getCustomCircleColor(),
                user.getCustomTargetColor(),
                user.getCustomBackgroundColor(),
                user.getCustomEffectType());
    }
}
