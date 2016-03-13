package com.gempukku.secsy.network.server;

import com.gempukku.secsy.entity.event.Event;

public class ClientConnectedEvent extends Event {
    public static final ClientConnectedEvent SINGLETON = new ClientConnectedEvent();

    private ClientConnectedEvent() {
    }
}
