package com.gempukku.secsy.network.client;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.context.util.PriorityCollection;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityEventListener;
import com.gempukku.secsy.entity.EntityListener;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.InternalEntityManager;
import com.gempukku.secsy.entity.SimpleEntity;
import com.gempukku.secsy.entity.SimpleEntityRef;
import com.gempukku.secsy.entity.component.ComponentManager;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.game.InternalGameLoopListener;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.secsy.entity.io.StoredEntityData;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = {EntityManager.class, InternalEntityManager.class, ServerEventBus.class})
public class RemoteEntityManager implements EntityManager, InternalEntityManager, ServerEventBus, LifeCycleSystem, InternalGameLoopListener {
    @In
    private ComponentManager componentManager;
    @In
    private InternalComponentManager internalComponentManager;
    @In
    private InternalGameLoop internalGameLoop;

    private PriorityCollection<EntityEventListener> listeners = new PriorityCollection<>();
    private PriorityCollection<EntityListener> entityListeners = new PriorityCollection<>();

    private Set<SimpleEntity> serverEntities = new HashSet<>();
    private Set<SimpleEntity> clientEntities = new HashSet<>();

    private int maxId;

    private ServerCommunication serverCommunication;

    private EntityListener dispatchEntityListener = new DispatchEntityListener();
    private EntityEventListener dispatchEntityEventListener = new DispatchEntityEventListener();

    public void setServerCommunication(ServerCommunication serverCommunication) {
        this.serverCommunication = serverCommunication;
    }

    @Override
    public void initialize() {
        internalGameLoop.addInternalGameLoopListener(this);
    }

    @Override
    public void addEntityEventListener(EntityEventListener entityEventListener) {
        listeners.add(entityEventListener);
    }

    @Override
    public void removeEntityEventListener(EntityEventListener entityEventListener) {
        listeners.remove(entityEventListener);
    }

    @Override
    public void addEntityListener(EntityListener entityListener) {
        entityListeners.add(entityListener);
    }

    @Override
    public void removeEntityListener(EntityListener entityListener) {
        entityListeners.remove(entityListener);
    }

    @Override
    public int getEntityId(EntityRef entityRef) {
        SimpleEntity entity = ((SimpleEntityRef) entityRef).getEntity();
        return entity.getEntityId();
    }

    @Override
    public String getEntityUniqueIdentifier(EntityRef entityRef) {
        SimpleEntity entity = ((SimpleEntityRef) entityRef).getEntity();
        if (serverEntities.contains(entity))
            return "s-" + entity.getEntityId();
        else
            return "c-" + entity.getEntityId();
    }

    @Override
    public void preUpdate() {
        serverCommunication.visitQueuedEvents(
                new ServerCommunication.ClientEventVisitor() {
                    @Override
                    public void visitEntityCreate(StoredEntityData entityData) {
                        SimpleEntity entity = new SimpleEntity(internalComponentManager, entityData.getEntityId());
                        Iterable<? extends ComponentData> components = entityData.getComponents();
                        components.forEach(
                                componentData -> {
                                    Class<? extends Component> componentClass = componentData.getComponentClass();
                                    Component component = internalComponentManager.createComponent(null, componentClass);
                                    componentData.getFields().entrySet().forEach(
                                            fieldNameAndValue -> internalComponentManager.setComponentFieldValue(component, fieldNameAndValue.getKey(), fieldNameAndValue.getValue()));
                                    entity.entityValues.put(componentClass, component);
                                });
                        serverEntities.add(entity);

                        Map<Class<? extends Component>, Component> addedComponents = new HashMap<>();
                        entity.entityValues.entrySet().forEach(
                                componentEntry -> addedComponents.put(componentEntry.getKey(), internalComponentManager.copyComponentUnmodifiable(componentEntry.getValue(), false)));

                        entityListeners.forEach(
                                listener -> listener.entitiesModified(Collections.singleton(entity)));
                        sendEventToEntity(entity, new AfterComponentAdded(addedComponents));
                    }

                    @Override
                    public void visitEntityUpdate(StoredEntityData entityData) {
                        SimpleEntity entity = getServerEntityById(entityData.getEntityId());

                        Map<Class<? extends Component>, Component> addedComponents = new HashMap<>();
                        Map<Class<? extends Component>, Component> updatedComponentsOld = new HashMap<>();
                        Map<Class<? extends Component>, Component> updatedComponentsNew = new HashMap<>();

                        Iterable<? extends ComponentData> components = entityData.getComponents();
                        components.forEach(
                                componentData -> {
                                    Class<? extends Component> componentClass = componentData.getComponentClass();
                                    if (entity.entityValues.containsKey(componentClass)) {
                                        Component component = entity.entityValues.get(componentClass);
                                        Component oldComponent = internalComponentManager.copyComponentUnmodifiable(component, false);
                                        // Just to be able to use variable passed to Lambda
                                        AtomicBoolean modified = new AtomicBoolean(false);
                                        componentData.getFields().entrySet().forEach(
                                                fieldNameAndValue -> {
                                                    Class<?> fieldClass = internalComponentManager.getComponentFieldTypes(component).get(fieldNameAndValue.getKey());
                                                    Object oldValue = internalComponentManager.getComponentFieldValue(component, fieldNameAndValue.getKey(), fieldClass);
                                                    Object newValue = fieldNameAndValue.getValue();
                                                    if (oldValue != newValue
                                                            && (oldValue == null || newValue == null || !oldValue.equals(newValue))) {
                                                        modified.set(true);
                                                        internalComponentManager.setComponentFieldValue(component, fieldNameAndValue.getKey(), newValue);
                                                    }
                                                });
                                        if (modified.get()) {
                                            Component newComponent = internalComponentManager.copyComponentUnmodifiable(component, false);
                                            updatedComponentsNew.put(componentClass, newComponent);
                                            updatedComponentsOld.put(componentClass, oldComponent);
                                        }
                                    } else {
                                        Component component = internalComponentManager.createComponent(null, componentClass);
                                        componentData.getFields().entrySet().forEach(
                                                fieldNameAndValue -> internalComponentManager.setComponentFieldValue(component, fieldNameAndValue.getKey(), fieldNameAndValue.getValue()));
                                        entity.entityValues.put(componentClass, component);
                                        addedComponents.put(componentClass, internalComponentManager.copyComponentUnmodifiable(component, false));
                                    }
                                });

                        Map<Class<? extends Component>, Component> removedComponents = new HashMap<>();
                        for (Map.Entry<Class<? extends Component>, Component> componentEntry : entity.entityValues.entrySet()) {
                            boolean contains = false;
                            for (ComponentData componentData : entityData.getComponents()) {
                                if (componentData.getComponentClass().equals(componentEntry.getKey())) {
                                    contains = true;
                                    break;
                                }
                            }
                            if (!contains) {
                                removedComponents.put(componentEntry.getKey(), componentEntry.getValue());
                            }
                        }

                        if (!removedComponents.isEmpty()) {
                            BeforeComponentRemoved event = new BeforeComponentRemoved(removedComponents);
                            sendEventToEntity(entity, event);
                        }

                        for (Class<? extends Component> componentClass : removedComponents.keySet()) {
                            entity.entityValues.remove(componentClass);
                        }

                        entityListeners.forEach(
                                listener -> listener.entitiesModified(Collections.singleton(entity)));

                        if (!addedComponents.isEmpty()) {
                            AfterComponentAdded event = new AfterComponentAdded(addedComponents);
                            sendEventToEntity(entity, event);
                        }

                        if (!updatedComponentsOld.isEmpty()) {
                            AfterComponentUpdated event = new AfterComponentUpdated(updatedComponentsOld, updatedComponentsNew);
                            sendEventToEntity(entity, event);
                        }
                    }

                    @Override
                    public void visitEntityRemove(int entityId) {
                        SimpleEntity entity = getServerEntityById(entityId);

                        Map<Class<? extends Component>, Component> removedComponents = new HashMap<>();
                        entity.entityValues.entrySet().forEach(
                                componentEntry -> removedComponents.put(componentEntry.getKey(), internalComponentManager.copyComponentUnmodifiable(componentEntry.getValue(), false)));
                        sendEventToEntity(entity, new BeforeComponentRemoved(removedComponents));

                        entity.exists = false;
                        serverEntities.remove(entity);

                        entityListeners.forEach(
                                listener -> listener.entitiesModified(Collections.singleton(entity)));
                    }

                    @Override
                    public void visitEventReceived(int entityId, Event event) {
                        sendEventToEntity(getServerEntityById(entityId), event);
                    }
                });
    }

    @Override
    public void postUpdate() {
        // Do nothing
    }

    private SimpleEntity getServerEntityById(int entityId) {
        for (SimpleEntity serverEntity : serverEntities) {
            if (serverEntity.getEntityId() == entityId)
                return serverEntity;
        }
        return null;
    }

    @Override
    public EntityRef createEntity() {
        SimpleEntity entity = new SimpleEntity(internalComponentManager, ++maxId);
        clientEntities.add(entity);
        return createSimpleEntityRef(entity, false);
    }

    @Override
    public EntityRef createEntity(EntityData entityData) {
        SimpleEntity entity = new SimpleEntity(internalComponentManager, ++maxId);
        addEntityDataToEntity(entityData, entity);
        clientEntities.add(entity);

        Map<Class<? extends Component>, Component> components = new HashMap<>();
        entity.entityValues.forEach(
                (clazz, component) -> components.put(clazz, internalComponentManager.copyComponentUnmodifiable(component, false)));

        entityListeners.forEach(
                listener -> listener.entitiesModified(Collections.singleton(entity)));

        sendEventToEntity(entity, new AfterComponentAdded(components));
        return createSimpleEntityRef(entity, false);
    }

    private void addEntityDataToEntity(EntityData entityData, SimpleEntity entity) {
        entityData.getComponents().forEach(
                componentData -> {
                    Class<? extends Component> componentClass = componentData.getComponentClass();
                    Component component = internalComponentManager.createComponent(null, componentClass);
                    componentData.getFields().entrySet().forEach(
                            fieldNameAndValue -> internalComponentManager.setComponentFieldValue(component, fieldNameAndValue.getKey(), fieldNameAndValue.getValue()));
                    entity.entityValues.put(componentClass, component);
                });
    }

    @Override
    public EntityRef wrapEntity(SimpleEntity entity) {
        return createSimpleEntityRef(entity, false);
    }

    @Override
    public EntityRef createNewEntityRef(EntityRef entityRef) {
        return createSimpleEntityRef(((SimpleEntityRef) entityRef).getEntity(), false);
    }

    @Override
    public boolean isSameEntity(EntityRef ref1, EntityRef ref2) {
        return ((SimpleEntityRef) ref1).getEntity() == ((SimpleEntityRef) ref2).getEntity();
    }

    @Override
    public Iterable<EntityRef> getEntitiesWithComponents(Class<? extends Component> component, Class<? extends Component>... additionalComponents) {
        return Iterables.concat(
                Iterables.transform(Iterables.filter(serverEntities,
                        entity -> {
                            if (!entity.entityValues.containsKey(component))
                                return false;

                            for (Class<? extends Component> additionalComponent : additionalComponents) {
                                if (!entity.entityValues.containsKey(additionalComponent))
                                    return false;
                            }

                            return true;
                        }),
                        entity -> createSimpleEntityRef(entity, false)),
                Iterables.transform(Iterables.filter(clientEntities,
                        entity -> {
                            if (!entity.entityValues.containsKey(component))
                                return false;

                            for (Class<? extends Component> additionalComponent : additionalComponents) {
                                if (!entity.entityValues.containsKey(additionalComponent))
                                    return false;
                            }

                            return true;
                        }),
                        entity -> createSimpleEntityRef(entity, false)));
    }

    @Override
    public void destroyEntity(EntityRef entityRef) {
        SimpleEntity underlyingEntity = ((SimpleEntityRef) entityRef).getEntity();
        if (serverEntities.contains(underlyingEntity))
            throw new IllegalStateException("Can't destroy a server entity");
        Collection<Class<? extends Component>> components = entityRef.listComponents();
        //noinspection unchecked
        entityRef.removeComponents(components.toArray(new Class[components.size()]));
        entityRef.saveChanges();
        underlyingEntity.exists = false;
        clientEntities.remove(underlyingEntity);

        entityListeners.forEach(
                listener -> listener.entitiesModified(Collections.singleton(underlyingEntity)));
    }

    @Override
    public void sendEventToServer(Event event) {
        serverCommunication.sendEventToServer(event);
    }

    private void sendEventToEntity(SimpleEntity entity, Event event) {
        listeners.forEach(entityListener -> entityListener.eventSent(createSimpleEntityRef(entity, false), event));
    }

    private SimpleEntityRef createSimpleEntityRef(SimpleEntity entity, boolean readOnly) {
        return new SimpleEntityRef(internalComponentManager, dispatchEntityListener, dispatchEntityEventListener,
                entity, readOnly);
    }

    private class DispatchEntityListener implements EntityListener {
        @Override
        public void entitiesModified(Iterable<SimpleEntity> entity) {
            entityListeners.forEach(
                    listener -> listener.entitiesModified(entity));
        }
    }

    private class DispatchEntityEventListener implements EntityEventListener {
        @Override
        public void eventSent(EntityRef entity, Event event) {
            listeners.forEach(
                    listener -> {
                        SimpleEntityRef newEntityRef = createSimpleEntityRef(((SimpleEntityRef) entity).getEntity(), false);
                        listener.eventSent(newEntityRef, event);
                    });
        }
    }
}
