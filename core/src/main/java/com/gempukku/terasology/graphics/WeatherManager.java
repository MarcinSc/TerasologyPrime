package com.gempukku.terasology.graphics;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.terasology.time.TimeManager;

@RegisterSystem(
        shared = SkyColorProvider.class)
public class WeatherManager implements SkyColorProvider {
    @In
    private TimeManager timeManager;

    @Override
    public Vector3 getSkyColor(String worldId, float x, float y, float z) {
        float radialTimeOfDay = (float) (2 * Math.PI * timeManager.getWorldDayTime(worldId));

        Vector3 skyColor = new Vector3(145f / 255, 186f / 255, 220f / 255);
        float dayComponent = (float) -Math.cos(radialTimeOfDay);
        if (dayComponent < -0.3f) {
            skyColor.scl(0);
        } else if (dayComponent < 0.3) {
            skyColor.scl((dayComponent + 0.3f) / (2f * 0.3f));
        }

        return skyColor;
    }
}
