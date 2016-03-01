package com.gempukku.secsy.entity.network.server;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityEventListener;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.InternalEntityManager;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentRemoved;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.event.BeforeEntityUnloaded;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.game.InternalGameLoopListener;
import com.gempukku.secsy.entity.network.ToClientEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = ClientManager.class)
public class ClientSystem implements ClientManager, EntityEventListener, LifeCycleSystem,
        ClientEntityRelevancyRuleListener, InternalGameLoopListener {
    @In
    private InternalEntityManager internalEntityManager;
    @In
    private InternalGameLoop internalGameLoop;

    private List<ClientEntityRelevanceRule> relevanceRuleList = new LinkedList<>();
    private List<EntityComponentFieldFilter> entityComponentFieldFilters = new LinkedList<>();

    private Map<String, ClientCommunication> connectedClients = new HashMap<>();
    private Map<String, EntityRef> clientEntityMap = new HashMap<>();
    private Map<String, Set<Integer>> entitiesClientIsAwareOf = new HashMap<>();

    @Override
    public void initialize() {
        internalEntityManager.addEntityEventListener(this);
        internalGameLoop.addInternalGameLoopListener(this);
    }

    @Override
    public void addClientEntityRelevanceRule(ClientEntityRelevanceRule clientEntityRelevanceRule) {
        relevanceRuleList.add(clientEntityRelevanceRule);
        clientEntityRelevanceRule.addClientEntityRelevancyRuleListener(this);
    }

    @Override
    public void removeClientEntityRelevanceRule(ClientEntityRelevanceRule clientEntityRelevanceRule) {
        clientEntityRelevanceRule.removeClientEntityRelevancyRuleListener(this);
        relevanceRuleList.remove(clientEntityRelevanceRule);
    }

    @Override
    public void addEntityComponentFieldFilter(EntityComponentFieldFilter entityComponentFieldFilter) {
        entityComponentFieldFilters.add(entityComponentFieldFilter);
    }

    @Override
    public void removeEntityComponentFieldFilter(EntityComponentFieldFilter entityComponentFieldFilter) {
        entityComponentFieldFilters.remove(entityComponentFieldFilter);
    }

    @Override
    public void preUpdate() {
        for (ClientCommunication clientCommunication : connectedClients.values()) {
            clientCommunication.commitChanges();
        }

        for (Map.Entry<String, ClientCommunication> clientChannels : connectedClients.entrySet()) {
            String clientId = clientChannels.getKey();
            EntityRef clientEntity = clientEntityMap.get(clientId);
            clientChannels.getValue().visitQueuedEvents(
                    event -> clientEntity.send(event));
        }
    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void eventSent(EntityRef entity, Event event) {
        if (event.getClass().isAnnotationPresent(ToClientEvent.class)) {
            int entityId = internalEntityManager.getEntityId(entity);
            for (Map.Entry<String, Set<Integer>> clientKnownEntities : entitiesClientIsAwareOf.entrySet()) {
                String clientId = clientKnownEntities.getKey();
                if (clientKnownEntities.getValue().contains(entityId)) {
                    connectedClients.get(clientId).sendEvent(entityId, event);
                }
            }
        }
    }

    @Override
    public void entityRelevancyChanged(String clientId, Iterable<EntityRef> entities) {
        EntityRef clientEntity = clientEntityMap.get(clientId);
        Set<Integer> entitiesClientKnows = entitiesClientIsAwareOf.get(clientId);
        ClientCommunication clientCommunication = connectedClients.get(clientId);

        for (EntityRef entity : entities) {
            int entityId = internalEntityManager.getEntityId(entity);
            boolean shouldBeKnown = shouldEntityBeKnownToClient(clientEntity, entity);
            boolean isKnown = entitiesClientKnows.contains(entityId);
            if (shouldBeKnown && !isKnown) {
                entitiesClientKnows.add(entityId);
                clientCommunication.addEntity(entityId, entity, entityComponentFieldFilters);
            } else if (!shouldBeKnown && isKnown) {
                clientCommunication.removeEntity(entityId);
                entitiesClientKnows.remove(entityId);
            }
        }
    }

    @ReceiveEvent
    public void componentsAdded(AfterComponentAdded event, EntityRef entity) {
        entityModified(entity);
    }

    @ReceiveEvent
    public void componentsUpdated(AfterComponentUpdated event, EntityRef entity) {
        entityModified(entity);
    }

    @ReceiveEvent
    public void componentsRemoved(AfterComponentRemoved event, EntityRef entity) {
        entityModified(entity);
    }

    @ReceiveEvent
    public void entityUnloaded(BeforeEntityUnloaded event, EntityRef entity) {
        int entityId = internalEntityManager.getEntityId(entity);
        for (Map.Entry<String, Set<Integer>> clientKnownEntities : entitiesClientIsAwareOf.entrySet()) {
            String clientId = clientKnownEntities.getKey();
            ClientCommunication clientCommunication = connectedClients.get(clientId);

            if (clientKnownEntities.getValue().remove(entityId)) {
                clientCommunication.removeEntity(entityId);
            }
        }
    }

    private void entityModified(EntityRef entity) {
        int entityId = internalEntityManager.getEntityId(entity);
        for (Map.Entry<String, Set<Integer>> clientKnownEntities : entitiesClientIsAwareOf.entrySet()) {
            String clientId = clientKnownEntities.getKey();
            EntityRef clientEntity = clientEntityMap.get(clientId);
            ClientCommunication clientCommunication = connectedClients.get(clientId);
            Set<Integer> entitiesKnownByClient = clientKnownEntities.getValue();

            boolean clientShouldKnow = shouldEntityBeKnownToClient(clientEntity, entity);
            boolean clientKnows = entitiesKnownByClient.contains(entityId);
            if (clientKnows && clientShouldKnow) {
                clientCommunication.updateEntity(entityId, entity, entityComponentFieldFilters);
            } else if (clientKnows && !clientShouldKnow) {
                clientCommunication.removeEntity(entityId);
                entitiesKnownByClient.remove(entityId);
            } else if (!clientKnows && clientShouldKnow) {
                clientCommunication.addEntity(entityId, entity, entityComponentFieldFilters);
                entitiesKnownByClient.add(entityId);
            }
        }
    }

    private boolean shouldEntityBeKnownToClient(EntityRef clientEntity, EntityRef entity) {
        for (ClientEntityRelevanceRule clientEntityRelevanceRule : relevanceRuleList) {
            if (clientEntityRelevanceRule.isEntityRelevantForClient(clientEntity, entity))
                return true;
        }
        return false;
    }

    @Override
    public void addClient(String clientId, EntityRef clientEntity, ClientCommunication clientCommunication) {
        clientEntityMap.put(clientId, clientEntity);
        connectedClients.put(clientId, clientCommunication);
        entitiesClientIsAwareOf.put(clientId, new HashSet<>());
    }

    @Override
    public void removeClient(String clientId) {
        clientEntityMap.remove(clientId);
        connectedClients.remove(clientId);
        entitiesClientIsAwareOf.remove(clientId);
    }
}
