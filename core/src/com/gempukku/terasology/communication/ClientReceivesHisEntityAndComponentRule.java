package com.gempukku.terasology.communication;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.context.util.PriorityCollection;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.network.server.ClientConnectedEvent;
import com.gempukku.secsy.entity.network.server.ClientEntityRelevanceRule;
import com.gempukku.secsy.entity.network.server.ClientEntityRelevancyRuleListener;
import com.gempukku.secsy.entity.network.server.ClientManager;
import com.gempukku.secsy.entity.network.server.EntityComponentFieldFilter;
import com.gempukku.terasology.world.component.ClientComponent;

import java.util.Collections;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class ClientReceivesHisEntityAndComponentRule implements ClientEntityRelevanceRule, EntityComponentFieldFilter,
        LifeCycleSystem {
    @In
    private ClientManager clientManager;
    @In
    private EntityManager entityManager;

    private PriorityCollection<ClientEntityRelevancyRuleListener> listeners = new PriorityCollection<>();

    @Override
    public void initialize() {
        clientManager.addClientEntityRelevanceRule(this);
        clientManager.addEntityComponentFieldFilter(this);
    }

    @Override
    public void addClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean isEntityRelevantForClient(EntityRef clientEntity, EntityRef entity) {
        return entityManager.isSameEntity(clientEntity, entity);
    }

    @ReceiveEvent
    public void clientConnected(ClientConnectedEvent event, EntityRef clientEntity, ClientComponent clientComponent) {
        for (ClientEntityRelevancyRuleListener listener : listeners) {
            listener.entityRelevancyChanged(clientComponent.getClientId(), Collections.singleton(clientEntity));
        }
    }

    @Override
    public boolean isComponentFieldRelevant(EntityRef clientEntity, EntityRef entity, Class<? extends Component> component, String field) {
        // Client should see all fields of his own ClientComponent
        return component == ClientComponent.class && entityManager.isSameEntity(clientEntity, entity);
    }

    @Override
    public boolean isComponentRelevant(EntityRef clientEntity, EntityRef entity, Class<? extends Component> component) {
        // Client should see his own ClientComponent
        return component == ClientComponent.class && entityManager.isSameEntity(clientEntity, entity);
    }
}
