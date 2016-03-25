package com.gempukku.terasology.graphics.environment.mesh;

import com.gempukku.secsy.entity.event.Event;

public class AfterChunkMeshCreated extends Event {
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    public AfterChunkMeshCreated(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
