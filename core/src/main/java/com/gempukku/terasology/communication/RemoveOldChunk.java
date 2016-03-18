package com.gempukku.terasology.communication;

import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.network.ToClientEvent;

@ToClientEvent
public class RemoveOldChunk extends Event {
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    public RemoveOldChunk(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
