package com.gempukku.terasology.communication;

import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.network.ToClientEvent;

@ToClientEvent
public class StoreNewChunk extends Event {
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;
    public final short[] blockIds;

    public StoreNewChunk(String worldId, int x, int y, int z, short[] blockIds) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockIds = blockIds;
    }
}
