package com.gempukku.terasology.celestialBodies;

import com.gempukku.terasology.celestialBodies.model.CelestialBody;

public interface CelestialBodyProvider {
    Iterable<CelestialBody> getVisibleCelestialBodies(String worldId, float x, float y, float z);
}
