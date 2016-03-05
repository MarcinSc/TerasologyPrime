package com.gempukku.secsy.entity.network.client;

import com.gempukku.secsy.entity.event.Event;

public interface ServerEventBus {
    void sendEventToServer(Event event);
}
