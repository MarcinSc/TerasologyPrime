package com.gempukku.terasology.world.chunk.event;

import com.gempukku.secsy.entity.event.Event;

public class BeforeChunkUnloadedEvent extends Event {
    public final static BeforeChunkUnloadedEvent INSTANCE = new BeforeChunkUnloadedEvent();

    private BeforeChunkUnloadedEvent() {
    }
}
