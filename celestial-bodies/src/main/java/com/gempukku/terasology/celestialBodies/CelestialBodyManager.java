package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;
import com.gempukku.terasology.time.TimeManager;

import java.util.Collections;

@RegisterSystem(
        shared = CelestialBodyProvider.class)
public class CelestialBodyManager implements CelestialBodyProvider {
    @In
    private TimeManager timeManager;

    @Override
    public Iterable<CelestialBody> getVisibleCelestialBodies(String worldId, float x, float y, float z) {
        int dayLengthInMs = 1 * 60 * 1000;

        // Number between 0 and 2*PI, where 0 is "midday", PI is midnight
        float timeOfDay = (float) (2 * Math.PI * (timeManager.getMultiverseTime() % dayLengthInMs) / (1f * dayLengthInMs));

        Vector3 directionFromViewpoint = new Vector3((float) Math.sin(timeOfDay), (float) Math.cos(timeOfDay), 0);
        Vector3 color = new Vector3(1.0f, 1.0f, 1.0f);
        float size = 0.02f;

        return Collections.singleton(new CelestialBody(color, directionFromViewpoint, size));
    }
}
