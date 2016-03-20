package com.gempukku.terasology.graphics;

import com.badlogic.gdx.math.Vector3;

public interface SkyColorProvider {
    /**
     * Retrieves the color of the sky in the given world, when looked at from given coordinates.
     *
     * @param worldId
     * @param x
     * @param y
     * @param z
     * @return
     */
    Vector3 getSkyColor(String worldId, float x, float y, float z);
}
