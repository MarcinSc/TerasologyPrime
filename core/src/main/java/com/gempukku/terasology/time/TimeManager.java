package com.gempukku.terasology.time;

public interface TimeManager {
    long getMultiverseTime();

    float getWorldDayTime(String worldId);

    long getTimeSinceLastUpdate();
}
