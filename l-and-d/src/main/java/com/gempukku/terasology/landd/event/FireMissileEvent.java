package com.gempukku.terasology.landd.event;

import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.network.ToClientEvent;

@ToClientEvent
public class FireMissileEvent extends Event {
    public final String worldId;
    public final float startX;
    public final float startY;
    public final float startZ;
    public final float endX;
    public final float endY;
    public final float endZ;
    public final float duration;

    public FireMissileEvent(String worldId, float startX, float startY, float startZ, float endX, float endY, float endZ, float duration) {
        this.worldId = worldId;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
        this.duration = duration;
    }
}
