package com.gempukku.secsy.network.client;

import com.gempukku.secsy.entity.event.Event;

public interface ServerEventBus {
    void sendEventToServer(Event event);
}
