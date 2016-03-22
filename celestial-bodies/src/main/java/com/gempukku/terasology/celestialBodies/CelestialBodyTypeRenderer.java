package com.gempukku.terasology.celestialBodies;

import com.gempukku.terasology.celestialBodies.model.CelestialBody;

public interface CelestialBodyTypeRenderer {
    String getShaderSnippet();

    int getDataFloatCount();

    Iterable<CelestialBody> getCelestialBodies(String worldId, float x, float y, float z);
}
