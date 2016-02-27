package com.gempukku.terasology.world.component;

import java.util.Map;

public enum TexturePart {

    Top("top", "topAndBottom", "all"),
    Bottom("bottom", "topAndBottom", "all"),
    East("left", "leftAndRight", "sides", "all"),
    West("right", "leftAndRight", "sides", "all"),
    South("front", "frontAndBack", "sides", "all"),
    North("back", "frontAndBack", "sides", "all");

    private String[] order;

    private TexturePart(String... order) {
        this.order = order;
    }

    public String extractTexture(Map<String, String> partsTextures) {
        for (String part : order) {
            String result = partsTextures.get(part);
            if (result != null)
                return result;
        }
        return null;
    }

    public String[] getOrder() {
        return order;
    }
}
