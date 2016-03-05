package com.gempukku.terasology.world.chunk.event;

import com.gempukku.secsy.entity.event.Event;

public class AfterChunkLoadedEvent extends Event {
    public final static AfterChunkLoadedEvent INSTANCE = new AfterChunkLoadedEvent();

    private AfterChunkLoadedEvent() {
    }
}
