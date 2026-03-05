package com.example.chasergame.utils;

import android.graphics.Color;
import com.example.chasergame.models.Skin;
import java.util.ArrayList;
import java.util.List;

public class SkinManager {
    private static List<Skin> skins;

    public static List<Skin> getSkins() {
        if (skins == null) {
            skins = new ArrayList<>();
            // Default Skin - Back to "none" for no effects
            skins.add(new Skin("default", "Classic Red", 0, Color.parseColor("#FFFFFF"), Color.parseColor("#B71C1C"), Color.parseColor("#1A1A1A"), "none"));
            
            // Neon Skin
            skins.add(new Skin("neon", "Neon Night", 100000, Color.parseColor("#00FBFF"), Color.parseColor("#00FBFF"), Color.parseColor("#000000"), "glow"));
            
            // Fire Skin
            skins.add(new Skin("fire", "Hellfire", 100000, Color.parseColor("#FF5722"), Color.parseColor("#FFC107"), Color.parseColor("#210000"), "particles"));
            
            // Ocean Skin
            skins.add(new Skin("ocean", "Deep Ocean", 100000, Color.parseColor("#00BCD4"), Color.parseColor("#03A9F4"), Color.parseColor("#001219"), "bubbles"));
            
            // Golden Skin
            skins.add(new Skin("gold", "Midas Touch", 100000, Color.parseColor("#FFD700"), Color.parseColor("#DAA520"), Color.parseColor("#1C1C1C"), "sparkle"));

            // Custom Skin Placeholder
            skins.add(new Skin("custom", "Custom Creation", 300000, Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), Color.parseColor("#333333"), "none"));
        }
        return skins;
    }

    public static Skin getSkinById(String id) {
        for (Skin skin : getSkins()) {
            if (skin.id.equals(id)) return skin;
        }
        return getSkins().get(0); // Return default if not found
    }
}
