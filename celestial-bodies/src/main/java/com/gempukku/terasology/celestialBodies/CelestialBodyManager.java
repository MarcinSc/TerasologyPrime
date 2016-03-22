package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;
import com.gempukku.terasology.celestialBodies.model.CircleCelestialBody;
import com.gempukku.terasology.time.TimeManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@RegisterSystem(
        shared = CelestialBodyProvider.class)
public class CelestialBodyManager implements CelestialBodyProvider {
    @In
    private TimeManager timeManager;

    @Override
    public Iterable<CelestialBody> getVisibleCelestialBodies(String worldId, float x, float y, float z) {
        Random rnd = new Random(1032);

        float timeOfDay = getTimeOfDay();

        List<CelestialBody> celestialBodies = new LinkedList<>();
        celestialBodies.add(getSun(timeOfDay));

        float alpha = 0f;
        float dayComponent = (float) Math.cos(timeOfDay);
        if (dayComponent < -0.3f) {
            alpha = 1f;
        } else if (dayComponent < 0.3) {
            alpha = 1 - (dayComponent + 0.3f) / (2f * 0.3f);
        }

        // If it's bright (from Sun), the stars are not visible
        if (alpha > 0) {
            for (int i = 0; i < 1000; i++) {
                float u = rnd.nextFloat() * 2 - 1;
                float theta = 2 * (float) Math.PI * rnd.nextFloat();

                float posX = (float) (Math.sqrt(1 - u * u) * Math.cos(theta - timeOfDay));
                float posY = (float) (Math.sqrt(1 - u * u) * Math.sin(theta - timeOfDay));
                float posZ = u;

                CircleCelestialBody star = new CircleCelestialBody(
                        new Color(1, 1, 1, alpha),
                        new Vector3(posX, posY, posZ).nor(),
                        0.002f);
                celestialBodies.add(star);
            }
        }

        return celestialBodies;
    }

    private CelestialBody getSun(float timeOfDay) {
        Vector3 directionFromViewpoint = new Vector3((float) Math.sin(timeOfDay), (float) Math.cos(timeOfDay), 0);

        return new CircleCelestialBody(new Color(1.0f, 1.0f, 1.0f, 1), directionFromViewpoint, 0.02f);
    }

    private boolean isDay(float timeOfDay) {
        return timeOfDay < Math.PI / 2f || timeOfDay > 3 * Math.PI / 2f;
    }

    private float getTimeOfDay() {
        int dayLengthInMs = 1 * 60 * 1000;

        // Number between 0 and 2*PI, where 0 is "midday", PI is midnight
        return (float) (2 * Math.PI * (timeManager.getMultiverseTime() % dayLengthInMs) / (1f * dayLengthInMs));
    }
}
