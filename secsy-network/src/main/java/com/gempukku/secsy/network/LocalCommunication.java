package com.gempukku.secsy.network;

import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.io.StoredEntityData;
import com.gempukku.secsy.network.client.ServerCommunication;
import com.gempukku.secsy.network.serialize.EntityInformation;
import com.gempukku.secsy.network.serialize.EntitySerializationUtil;
import com.gempukku.secsy.network.server.ClientCommunication;
import com.gempukku.secsy.network.server.EntityComponentFieldFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class LocalCommunication implements ServerCommunication, ClientCommunication {
    // The following two fields are shared by both server and client threads, therefore you have
    // to access them in a thread safe way
    private final LinkedList<Event> eventsToServer = new LinkedList<>();
    private final LinkedList<Collection<ClientEvent>> eventsToClientFrames = new LinkedList<>();

    // This object is used only by the server thread, as it builds a whole frame of events to be
    // processed by the client, the whole frame is then added to the eventsToClientFrames list.
    private List<ClientEvent> clientEventsBuildList = new LinkedList<>();

    private InternalComponentManager serversInternalComponentManager;
    private EntityRef serversClientEntity;

    public LocalCommunication(InternalComponentManager serversInternalComponentManager, EntityRef serversClientEntity) {
        this.serversInternalComponentManager = serversInternalComponentManager;
        this.serversClientEntity = serversClientEntity;
    }

    @Override
    public void visitQueuedEvents(ServerEventVisitor visitor) {
        Event event;
        do {
            synchronized (eventsToServer) {
                event = eventsToServer.poll();
            }
            if (event != null)
                visitor.visitEventReceived(event);
        } while (event != null);
    }

    @Override
    public void visitQueuedEvents(ClientEventVisitor visitor) {
        Collection<ClientEvent> events;
        do {
            synchronized (eventsToClientFrames) {
                events = eventsToClientFrames.poll();
            }
            if (events != null) {
                for (ClientEvent event : events) {
                    event.processForVisitor(visitor);
                }
            }
        } while (events != null);
    }

    @Override
    public void sendEventToClient(int entityId, Event event) throws IOException {
        clientEventsBuildList.add(receiveEventClientEvent(entityId, event));
    }

    @Override
    public void addEntity(int entityId, EntityRef entity, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) throws IOException {
        EntityInformation entityInformation = EntitySerializationUtil.serializeEntity(serversInternalComponentManager, serversClientEntity, entityId, entity, componentFieldFilters);
        clientEventsBuildList.add(createEntityClientEvent(entityInformation));
    }

    @Override
    public void updateEntity(int entityId, EntityRef entity, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) throws IOException {
        EntityInformation entityInformation = EntitySerializationUtil.serializeEntity(serversInternalComponentManager, serversClientEntity, entityId, entity, componentFieldFilters);
        clientEventsBuildList.add(updateEntityClientEvent(entityInformation));
    }

    @Override
    public void removeEntity(int entityId) throws IOException {
        clientEventsBuildList.add(removeEntityClientEvent(entityId));
    }

    @Override
    public void commitChanges() throws IOException {
        if (!clientEventsBuildList.isEmpty()) {
            synchronized (eventsToClientFrames) {
                eventsToClientFrames.add(clientEventsBuildList);
            }
            clientEventsBuildList = new LinkedList<>();
        }
    }

    @Override
    public void sendEventToServer(Event event) {
        // We do not copy the event object, assume that every data in it is immutable (for now)
        synchronized (eventsToServer) {
            eventsToServer.add(event);
        }
    }

    private static ClientEvent createEntityClientEvent(StoredEntityData entityData) {
        return new ClientEvent(0, 0, entityData, null);
    }

    private static ClientEvent updateEntityClientEvent(StoredEntityData entityData) {
        return new ClientEvent(1, 0, entityData, null);
    }

    private static ClientEvent removeEntityClientEvent(int entityId) {
        return new ClientEvent(2, entityId, null, null);
    }

    private static ClientEvent receiveEventClientEvent(int entityId, Event event) {
        return new ClientEvent(3, entityId, null, event);
    }

    private static class ClientEvent {
        private int type;
        private StoredEntityData entityData;
        private int entityId;
        private Event event;

        public ClientEvent(int type, int entityId, StoredEntityData entityData, Event event) {
            this.type = type;
            this.entityId = entityId;
            this.entityData = entityData;
            this.event = event;
        }

        private void processForVisitor(ClientEventVisitor visitor) {
            if (type == 0)
                visitor.visitEntityCreate(entityData);
            else if (type == 1)
                visitor.visitEntityUpdate(entityData);
            else if (type == 2)
                visitor.visitEntityRemove(entityId);
            else if (type == 3)
                visitor.visitEventReceived(entityId, event);
        }
    }
}
