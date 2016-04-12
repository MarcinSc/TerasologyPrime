package com.gempukku.secsy.network.server;

import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.ClassSystemProducer;
import com.gempukku.secsy.context.system.ShareSystemInitializer;
import com.gempukku.secsy.context.system.SimpleContext;
import com.gempukku.secsy.entity.EntityEventListener;
import com.gempukku.secsy.entity.EntityListener;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.InternalEntityManager;
import com.gempukku.secsy.entity.SampleEvent;
import com.gempukku.secsy.entity.SimpleEntity;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentRemoved;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.secsy.entity.event.BeforeEntityUnloaded;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.game.InternalGameLoopListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientSystemTest {
    private ClientSystem clientSystem;
    private MockInternalEntityManager mockInternalEntityManager;

    @Before
    public void setup() {
        ClassSystemProducer classSystemProducer = new ClassSystemProducer();
        classSystemProducer.addClass(ClientSystem.class);
        classSystemProducer.addClass(MockInternalEntityManager.class);
        classSystemProducer.addClass(MockInternalGameLoop.class);

        SimpleContext<Object> simpleContext = new SimpleContext<>();
        simpleContext.setSystemProducer(classSystemProducer);
        simpleContext.setSystemInitializer(new ShareSystemInitializer<>());

        simpleContext.startup();

        clientSystem = (ClientSystem) simpleContext.getSystem(ClientManager.class);
        mockInternalEntityManager = (MockInternalEntityManager) simpleContext.getSystem(InternalEntityManager.class);
    }

    @Test
    public void clientHasNoKnowledgeOfIrrelevantEntities() {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);
        EntityRef irrelevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(irrelevantEntity, 1);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        clientSystem.eventSent(irrelevantEntity, new AfterComponentAdded(Collections.emptyMap()));
        clientSystem.eventSent(irrelevantEntity, new AfterComponentUpdated(Collections.emptyMap(), Collections.emptyMap()));
        clientSystem.eventSent(irrelevantEntity, new BeforeComponentRemoved(Collections.emptyMap()));

        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    @Test
    public void clientIsAwareOfRelevantEntities() throws IOException {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);

        SimpleClientEntityRelevanceRule relevanceRule = new SimpleClientEntityRelevanceRule();
        clientSystem.addClientEntityRelevanceRule(relevanceRule);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        EntityRef relevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(relevantEntity, 1);

        relevanceRule.setEntityRelevant("clientId", relevantEntity, true);
        Mockito.verify(clientCommunication).addEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        clientSystem.eventSent(relevantEntity, new AfterComponentAdded(Collections.emptyMap()));
        Mockito.verify(clientCommunication, new Times(1)).updateEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        clientSystem.eventSent(relevantEntity, new AfterComponentUpdated(Collections.emptyMap(), Collections.emptyMap()));
        Mockito.verify(clientCommunication, new Times(2)).updateEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        clientSystem.eventSent(relevantEntity, new AfterComponentRemoved(Collections.emptyMap()));
        Mockito.verify(clientCommunication, new Times(3)).updateEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        relevanceRule.setEntityRelevant("clientId", relevantEntity, false);
        Mockito.verify(clientCommunication).removeEntity(Mockito.eq(1));
        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    @Test
    public void clientNotAwareOfClientEventsOnIrrelevantEntity() {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);
        EntityRef irrelevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(irrelevantEntity, 1);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        clientSystem.eventSent(irrelevantEntity, new EventRelevantToClient());
        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    @Test
    public void clientNotAwareOfNotClientEventsOnRelevantEntity() throws IOException {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);

        SimpleClientEntityRelevanceRule relevanceRule = new SimpleClientEntityRelevanceRule();
        clientSystem.addClientEntityRelevanceRule(relevanceRule);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        EntityRef relevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(relevantEntity, 1);

        relevanceRule.setEntityRelevant("clientId", relevantEntity, true);
        Mockito.verify(clientCommunication).addEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        clientSystem.eventSent(relevantEntity, new SampleEvent());
        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    @Test
    public void clientIsAwareOfClientEventsOnRelevantEntity() throws IOException {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);

        SimpleClientEntityRelevanceRule relevanceRule = new SimpleClientEntityRelevanceRule();
        clientSystem.addClientEntityRelevanceRule(relevanceRule);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        EntityRef relevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(relevantEntity, 1);

        relevanceRule.setEntityRelevant("clientId", relevantEntity, true);
        Mockito.verify(clientCommunication).addEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        EventRelevantToClient event = new EventRelevantToClient();
        clientSystem.eventSent(relevantEntity, event);
        Mockito.verify(clientCommunication).sendEventToClient(Mockito.eq(1), Mockito.same(event));
        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    @Test
    public void clientIsNotifiedIfEntityTheyWatchIsUnloaded() throws IOException {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);

        SimpleClientEntityRelevanceRule relevanceRule = new SimpleClientEntityRelevanceRule();
        clientSystem.addClientEntityRelevanceRule(relevanceRule);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        EntityRef relevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(relevantEntity, 1);

        relevanceRule.setEntityRelevant("clientId", relevantEntity, true);
        Mockito.verify(clientCommunication).addEntity(Mockito.eq(1), Mockito.same(relevantEntity), Mockito.any());

        clientSystem.eventSent(relevantEntity, new BeforeEntityUnloaded(Collections.emptyMap()));
        Mockito.verify(clientCommunication).removeEntity(1);
        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    @Test
    public void clientsAreNotifiedOfFrameEndAndTheirPendingEventsAreProcessed() throws IOException {
        EntityRef clientEntity = Mockito.mock(EntityRef.class);
        SampleEvent event = new SampleEvent();
        ClientCommunication clientCommunication = Mockito.mock(ClientCommunication.class);
        Mockito.doAnswer(
                invocation -> {
                    ClientCommunication.ServerEventVisitor serverEventVisitor = (ClientCommunication.ServerEventVisitor) invocation.getArguments()[0];
                    serverEventVisitor.visitEventReceived(event);
                    return null;
                }
        ).when(clientCommunication).visitQueuedEvents(Mockito.any());

        EntityRef irrelevantEntity = Mockito.mock(EntityRef.class);
        mockInternalEntityManager.setEntityId(irrelevantEntity, 1);

        clientSystem.addClient("clientId", clientEntity, clientCommunication);

        clientSystem.preUpdate();
        Mockito.verify(clientCommunication).commitChanges();
        Mockito.verify(clientCommunication).visitQueuedEvents(Mockito.any());
        Mockito.verify(clientEntity).send(Mockito.same(event));
        Mockito.verify(clientEntity).send(ClientConnectedEvent.SINGLETON);

        Mockito.verifyNoMoreInteractions(clientEntity, clientCommunication);
    }

    private static class SimpleClientEntityRelevanceRule implements ClientEntityRelevanceRule {
        private List<ClientEntityRelevancyRuleListener> listeners = new LinkedList<>();
        private Set<EntityRef> relevantEntities = new HashSet<>();

        private void setEntityRelevant(String clientId, EntityRef entityRef, boolean relevant) {
            if (relevant) {
                if (relevantEntities.add(entityRef)) {
                    for (ClientEntityRelevancyRuleListener listener : listeners) {
                        listener.entityRelevancyChanged(clientId, Collections.singleton(entityRef));
                    }
                }
            } else {
                if (relevantEntities.remove(entityRef)) {
                    for (ClientEntityRelevancyRuleListener listener : listeners) {
                        listener.entityRelevancyChanged(clientId, Collections.singleton(entityRef));
                    }
                }
            }
        }

        @Override
        public void addClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener) {
            listeners.add(listener);
        }

        @Override
        public boolean isEntityRelevantForClient(EntityRef clientEntity, EntityRef entity) {
            return relevantEntities.contains(entity);
        }

        @Override
        public void removeClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener) {
            listeners.remove(listener);
        }
    }

    @RegisterSystem(
            shared = InternalGameLoop.class)
    public static class MockInternalGameLoop implements InternalGameLoop {
        @Override
        public void addInternalGameLoopListener(InternalGameLoopListener internalGameLoopListener) {

        }

        @Override
        public void removeInternalGameLooplListener(InternalGameLoopListener internalGameLoopListener) {

        }

        @Override
        public void processUpdate() {

        }
    }

    @RegisterSystem(
            shared = InternalEntityManager.class)
    public static class MockInternalEntityManager implements InternalEntityManager {
        private Map<EntityRef, Integer> entityIds = new HashMap<>();

        public void setEntityId(EntityRef entity, int entityId) {
            entityIds.put(entity, entityId);
        }

        @Override
        public void addEntityEventListener(EntityEventListener entityEventListener) {

        }

        @Override
        public void removeEntityEventListener(EntityEventListener entityEventListener) {

        }

        @Override
        public void addEntityListener(EntityListener entityListener) {

        }

        @Override
        public void removeEntityListener(EntityListener entityListener) {

        }

        @Override
        public EntityRef wrapEntity(SimpleEntity entity) {
            return null;
        }

        @Override
        public int getEntityId(EntityRef entityRef) {
            return entityIds.get(entityRef);
        }
    }
}