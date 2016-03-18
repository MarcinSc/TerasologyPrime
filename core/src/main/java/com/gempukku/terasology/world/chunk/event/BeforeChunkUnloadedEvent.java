package com.gempukku.terasology.world.chunk.event;

import com.gempukku.secsy.entity.event.Event;

public class BeforeChunkUnloadedEvent extends Event {
    public final int x;
    public final int y;
    public final int z;

    public BeforeChunkUnloadedEvent(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
