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
import com.gempukku.secsy.entity.component.ComponentManager;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentRemoved;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.game.InternalGameLoopListener;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
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
        SimpleEntity entity = ((EntityRefImpl) entityRef).entity;
        return entity.getEntityId();
    }

    @Override
    public String getEntityUniqueIdentifier(EntityRef entityRef) {
        SimpleEntity entity = ((EntityRefImpl) entityRef).entity;
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
                    public void visitEntityCreate(EntityData entityData) {
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
                    public void visitEntityUpdate(EntityData entityData) {
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
        return new EntityRefImpl(entity, false);
    }

    @Override
    public EntityRef wrapEntity(SimpleEntity entity) {
        return new EntityRefImpl(entity, false);
    }

    @Override
    public EntityRef createNewEntityRef(EntityRef entityRef) {
        return new EntityRefImpl(((EntityRefImpl) entityRef).entity, false);
    }

    @Override
    public boolean isSameEntity(EntityRef ref1, EntityRef ref2) {
        return ((EntityRefImpl) ref1).entity == ((EntityRefImpl) ref2).entity;
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
                        entity -> new EntityRefImpl(entity, false)),
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
                        entity -> new EntityRefImpl(entity, false)));
    }

    @Override
    public void destroyEntity(EntityRef entityRef) {
        SimpleEntity underlyingEntity = ((EntityRefImpl) entityRef).entity;
        if (serverEntities.contains(underlyingEntity))
            throw new IllegalStateException("Can't destroy a server entity");
        Collection<Class<? extends Component>> components = entityRef.listComponents();
        //noinspection unchecked
        entityRef.removeComponents(components.toArray(new Class[components.size()]));
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
        listeners.forEach(entityListener -> entityListener.eventSent(new EntityRefImpl(entity, false), event));
    }

    @SuppressWarnings("unchecked")
    private class EntityRefImpl implements EntityRef {
        private SimpleEntity entity;
        private Map<Class<? extends Component>, Boolean> newInThisEntityRef = new HashMap<>();
        private Map<Class<? extends Component>, Component> accessibleComponents = new HashMap<>();
        private boolean readOnly;

        public EntityRefImpl(SimpleEntity entity, boolean readOnly) {
            this.entity = entity;
            this.readOnly = readOnly;
        }

        @Override
        public <T extends Component> T createComponent(Class<T> clazz) {
            validateWritable();
            if (accessibleComponents.containsKey(clazz))
                throw new IllegalStateException("This entity ref already has this component defined");
            if (entity.entityValues.containsKey(clazz))
                throw new IllegalStateException("This entity already has this component defined");

            T component = internalComponentManager.createComponent(this, clazz);
            newInThisEntityRef.put(clazz, true);
            accessibleComponents.put(clazz, component);

            return component;
        }

        @Override
        public <T extends Component> T getComponent(Class<T> clazz) {
            // First check if this EntityRef already has a component of that class to work with
            Component component = accessibleComponents.get(clazz);
            if (component != null)
                return (T) component;

            T originalComponent = (T) entity.entityValues.get(clazz);
            if (originalComponent == null)
                return null;

            T localComponent = internalComponentManager.copyComponent(this, originalComponent);
            if (readOnly)
                localComponent = internalComponentManager.copyComponentUnmodifiable(localComponent, true);
            accessibleComponents.put(clazz, localComponent);
            newInThisEntityRef.put(clazz, false);
            return localComponent;
        }

        @Override
        public void saveComponents(Component... components) {
            validateWritable();
            for (Component component : components) {
                if (internalComponentManager.getComponentEntity(component) != this)
                    throw new IllegalStateException("The component " + internalComponentManager.getComponentClass(component).getName() + " does not belong to this EntityRef");
            }

            for (Component component : components) {
                Class<? extends Component> clazz = internalComponentManager.getComponentClass(component);
                if (newInThisEntityRef.get(clazz)) {
                    if (entity.entityValues.containsKey(clazz))
                        throw new IllegalStateException("This entity already contains a component of that class");
                } else {
                    if (!entity.entityValues.containsKey(clazz))
                        throw new IllegalStateException("This entity does not contain a component of that class");
                }
            }

            Map<Class<? extends Component>, Component> addedComponents = new HashMap<>();

            for (Component component : components) {
                final Class<Component> clazz = internalComponentManager.getComponentClass(component);
                if (newInThisEntityRef.get(clazz)) {
                    Component storedComponent = internalComponentManager.copyComponent(null, component);
                    entity.entityValues.put(clazz, storedComponent);

                    internalComponentManager.saveComponent(storedComponent, component);

                    addedComponents.put(clazz, internalComponentManager.copyComponentUnmodifiable(component, false));
                }
            }

            Map<Class<? extends Component>, Component> updatedComponentsOld = new HashMap<>();
            Map<Class<? extends Component>, Component> updatedComponentsNew = new HashMap<>();

            for (Component component : components) {
                final Class<Component> clazz = internalComponentManager.getComponentClass(component);
                if (!newInThisEntityRef.get(clazz)) {
                    Component originalComponent = entity.entityValues.get(clazz);

                    updatedComponentsOld.put(clazz, internalComponentManager.copyComponentUnmodifiable(originalComponent, false));

                    internalComponentManager.saveComponent(originalComponent, component);

                    updatedComponentsNew.put(clazz, internalComponentManager.copyComponentUnmodifiable(originalComponent, false));
                }
            }

            addedComponents.keySet().forEach(
                    clazz -> newInThisEntityRef.put(clazz, false));

            entityListeners.forEach(
                    listener -> listener.entitiesModified(Collections.singleton(entity)));

            if (!addedComponents.isEmpty()) {
                AfterComponentAdded event = new AfterComponentAdded(addedComponents);
                createNewEntityRef(this).send(event);
            }

            if (!updatedComponentsOld.isEmpty()) {
                AfterComponentUpdated event = new AfterComponentUpdated(updatedComponentsOld, updatedComponentsNew);
                createNewEntityRef(this).send(event);
            }
        }

        @Override
        public <T extends Component> void removeComponents(Class<T>... clazz) {
            validateWritable();
            Map<Class<? extends Component>, Component> removedComponents = new HashMap<>();

            for (Class<T> componentClass : clazz) {
                Component originalComponent = entity.entityValues.get(componentClass);
                if (originalComponent == null)
                    throw new IllegalStateException("This entity does not contain a component of that class");

                removedComponents.put(componentClass, internalComponentManager.copyComponentUnmodifiable(originalComponent, false));
            }

            BeforeComponentRemoved event = new BeforeComponentRemoved(removedComponents);
            createNewEntityRef(this).send(event);

            for (Class<T> componentClass : clazz) {
                accessibleComponents.remove(clazz);
                newInThisEntityRef.remove(clazz);
                entity.entityValues.remove(componentClass);
            }

            entityListeners.forEach(
                    listener -> listener.entitiesModified(Collections.singleton(entity)));

            AfterComponentRemoved afterEvent = new AfterComponentRemoved(removedComponents);
            createNewEntityRef(this).send(afterEvent);
        }

        @Override
        public Collection<Class<? extends Component>> listComponents() {
            return Collections.unmodifiableCollection(entity.entityValues.keySet());
        }

        @Override
        public boolean hasComponent(Class<? extends Component> component) {
            return entity.entityValues.containsKey(component);
        }

        @Override
        public boolean exists() {
            return entity.exists;
        }

        @Override
        public void send(Event event) {
            validateWritable();
            sendEventToEntity(entity, event);
        }

        private void validateWritable() {
            if (readOnly)
                throw new IllegalStateException("This entity is in read only mode");
        }
    }
}
