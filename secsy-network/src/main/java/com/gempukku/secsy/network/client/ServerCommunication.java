package com.gempukku.secsy.network.client;

import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.io.StoredEntityData;

/**
 * Interface allowing client to communicate with the server.
 * It's responsibility is to transfer the relevant events that have happened on the client to server context.
 * It also allows to consume any entity changes and events server has sent to the client.
 */
public interface ServerCommunication {
    /**
     * Allows to process all pending changes that server has sent.
     *
     * @param visitor
     */
    void visitQueuedEvents(ClientEventVisitor visitor);

    /**
     * Allows to send an event to the server for processing in its context.
     *
     * @param entityId
     * @param event
     */
    void sendEventToServer(Event event);

    interface ClientEventVisitor {
        void visitEntityCreate(StoredEntityData entityData);

        void visitEntityUpdate(StoredEntityData entityData);

        void visitEntityRemove(int entityId);

        void visitEventReceived(int entityId, Event event);
    }
}
