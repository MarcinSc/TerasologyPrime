package com.gempukku.terasology.celestialBodies.model;

import com.badlogic.gdx.graphics.Camera;

public interface CelestialBody {
    int getFloatCount();

    boolean isVisibleFrom(Camera camera);

    void appendFloats(float[] array, int startIndex, Camera camera);
}
