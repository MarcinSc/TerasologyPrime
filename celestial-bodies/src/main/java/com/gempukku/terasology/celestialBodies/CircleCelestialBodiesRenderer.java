package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;
import com.gempukku.terasology.celestialBodies.model.CircleCelestialBody;
import com.gempukku.terasology.time.TimeManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@RegisterSystem(
        profiles = NetProfiles.CLIENT)
public class CircleCelestialBodiesRenderer implements CelestialBodyTypeRenderer, LifeCycleSystem {
    @In
    private CelestialBodyTypeRendererRegistry celestialBodyTypeRendererRegistry;
    @In
    private TimeManager timeManager;

    private int rendererIndex;

    @Override
    public void initialize() {
        rendererIndex = celestialBodyTypeRendererRegistry.registerCelestialBodyTypeRenderer(this);
    }

    @Override
    public String getShaderSnippet() {
        return "    vec2 bodyPos = vec2(u_celestialBodiesParams[arrayIndex + 0], u_celestialBodiesParams[arrayIndex + 1]);\n" +
                "    bodyPos.x *= aspectRatio;\n" +
                "\n" +
                "    float size = u_celestialBodiesParams[arrayIndex + 6];\n" +
                "    float maxSize = u_celestialBodiesParams[arrayIndex + 7];\n" +
                "    //check if it is in the radius of the star\n" +
                "    float distance = length(fragmentScreenCoords - bodyPos);\n" +
                "    if (distance < maxSize) {\n" +
                "        return vec4(\n" +
                "            u_celestialBodiesParams[arrayIndex + 2],\n" +
                "            u_celestialBodiesParams[arrayIndex + 3],\n" +
                "            u_celestialBodiesParams[arrayIndex + 4],\n" +
                "            (1.0 - smoothstep(size, maxSize, distance)) * u_celestialBodiesParams[arrayIndex + 5]);\n" +
                "    } else {\n" +
                "        return vec4(0.0, 0.0, 0.0, 0.0);\n" +
                "    }\n";
    }

    @Override
    public int getDataFloatCount() {
        return 8;
    }

    @Override
    public Iterable<CelestialBody> getCelestialBodies(String worldId, float x, float y, float z) {
        Random rnd = new Random(1032);

        float radialTimeOfDay = (float) (2 * Math.PI * timeManager.getWorldDayTime(worldId));

        List<CelestialBody> celestialBodies = new LinkedList<>();
        celestialBodies.add(getSun(radialTimeOfDay));

        float alpha = 0f;
        float dayComponent = (float) -Math.cos(radialTimeOfDay);
        if (dayComponent < -0.3f) {
            alpha = 1f;
        } else if (dayComponent < 0.3) {
            alpha = 1 - (dayComponent + 0.3f) / (2f * 0.3f);
        }

        // If it's bright (from Sun), the stars are not visible
        if (alpha > 0) {
            for (int i = 0; i < 300; i++) {
                float u = rnd.nextFloat() * 2 - 1;
                float theta = 2 * (float) Math.PI * rnd.nextFloat();

                float posX = (float) (Math.sqrt(1 - u * u) * Math.cos(theta - radialTimeOfDay));
                float posY = (float) (Math.sqrt(1 - u * u) * Math.sin(theta - radialTimeOfDay));
                float posZ = u;

                // Pulsar are 2% of stars, so if it's a pulsar and should not be displayed just skip it
                if (rnd.nextFloat() >= 0.02f || ((radialTimeOfDay % 0.2f) >= 0.05f)) {
                    CircleCelestialBody star = new CircleCelestialBody(
                            rendererIndex,
                            new Color(1, 1, 1, alpha),
                            new Vector3(posX, posY, posZ).nor(),
                            0.003f, 0.001f);
                    celestialBodies.add(star);
                }
            }
        }

        return celestialBodies;
    }

    private CelestialBody getSun(float timeOfDay) {
        Vector3 directionFromViewpoint = new Vector3((float) -Math.sin(timeOfDay), (float) -Math.cos(timeOfDay), 0);

        return new CircleCelestialBody(rendererIndex, new Color(
                1.0f, 249f / 255f, 210f / 255f, 1), directionFromViewpoint, 0.02f, 0.03f);
    }

    private boolean isDay(float timeOfDay) {
        return timeOfDay < Math.PI / 2f || timeOfDay > 3 * Math.PI / 2f;
    }
}
