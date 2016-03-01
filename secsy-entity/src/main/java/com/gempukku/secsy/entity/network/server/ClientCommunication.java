package com.gempukku.secsy.entity.network.server;

import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.event.Event;

import java.util.Collection;

/**
 * Interface allowing server (host) to communicate with connected clients (remote or local).
 * It's responsibility is to transfer the relevant changes that have happened on the server to client contexts.
 * It also allows to consume any events clients have sent to the server.
 */
public interface ClientCommunication {
    /**
     * Called to notify the client that the entity that there is a new entity relevant to them.
     * This method is fed filters that specify which components and which filters are to be transferred to the client
     * context.
     *
     * @param entityId
     * @param entity
     * @param componentFieldFilters
     */
    void addEntity(int entityId, EntityRef entity, Collection<? extends EntityComponentFieldFilter> componentFieldFilters);

    /**
     * Called to notify the client that the entity that is relevant to them, has been updated.
     * This method is fed filters that specify which components and which filters are to be transferred to the client
     * context.
     *
     * @param entityId
     * @param entity
     * @param componentFieldFilters
     */
    void updateEntity(int entityId, EntityRef entity, Collection<? extends EntityComponentFieldFilter> componentFieldFilters);

    /**
     * Called to notify the client that the entity that was relevant to them, has been removed or is no longer relevant
     * to them.
     *
     * @param entityId
     */
    void removeEntity(int entityId);

    /**
     * Called to notify the client, that the entity that is relevant to them has received an event that is relevant to them.
     *
     * @param entityId
     * @param entity
     * @param event
     */
    void sendEvent(int entityId, Event event);

    /**
     * This method is executed after this interface has been fed all the information it needs to transfer to the client
     * in one "frame". Client context should process all the notifications (above) in one swoop, when this method is called.
     */
    void commitChanges();

    /**
     * Allows server to process all the events this client has sent to it.
     *
     * @param visitor
     */
    void visitQueuedEvents(ServerEventVisitor visitor);

    interface ServerEventVisitor {
        void visitEventReceived(Event event);
    }
}
