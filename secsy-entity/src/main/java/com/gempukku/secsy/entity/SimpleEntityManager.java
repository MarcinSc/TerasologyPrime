package com.gempukku.secsy.entity;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.context.util.PriorityCollection;
import com.gempukku.secsy.entity.component.ComponentManager;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentRemoved;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.event.AfterEntityLoaded;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.secsy.entity.event.BeforeEntityUnloaded;
import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.game.InternalGameLoopListener;
import com.gempukku.secsy.entity.relevance.EntityRelevanceRule;
import com.gempukku.secsy.entity.relevance.EntityRelevanceRuleRegistry;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RegisterSystem(profiles = NetProfiles.AUTHORITY, shared = {EntityManager.class, InternalEntityManager.class, EntityRelevanceRuleRegistry.class})
public class SimpleEntityManager implements EntityManager, InternalEntityManager,
        EntityRelevanceRuleRegistry, LifeCycleSystem, InternalGameLoopListener {
    @In
    private ComponentManager componentManager;
    @In
    private InternalComponentManager internalComponentManager;
    @In
    private InternalGameLoop internalGameLoop;

    private PriorityCollection<EntityEventListener> entityEventListeners = new PriorityCollection<>();
    private PriorityCollection<EntityListener> entityListeners = new PriorityCollection<>();
    private Set<EntityRelevanceRule> entityRelevanceRules = new HashSet<>();

    private int maxId;
    private Set<SimpleEntity> entities = new HashSet<>();

    @Override
    public void addEntityEventListener(EntityEventListener entityEventListener) {
        entityEventListeners.add(entityEventListener);
    }

    @Override
    public void removeEntityEventListener(EntityEventListener entityEventListener) {
        entityEventListeners.remove(entityEventListener);
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
    public void registerEntityRelevanceRule(EntityRelevanceRule entityRelevanceRule) {
        entityRelevanceRules.add(entityRelevanceRule);
    }

    @Override
    public void deregisterEntityRelevanceRule(EntityRelevanceRule entityRelevanceRule) {
        entityRelevanceRules.remove(entityRelevanceRule);
    }

    @Override
    public void initialize() {
        internalGameLoop.addInternalGameLoopListener(this);
    }

    @Override
    public void destroy() {
        internalGameLoop.removeInternalGameLooplListener(this);
    }

    @Override
    public void preUpdate() {
        // Do nothing
    }

    /**
     * This method unloads all irrelevant entities
     */
    @Override
    public void postUpdate() {
        // First go through all the registered rules and tell them to update
        // their internal rules
        entityRelevanceRules.forEach(EntityRelevanceRule::determineRelevance);

        // Determine, which entities are unloaded for which rule
        Multimap<EntityRelevanceRule, SimpleEntity> entitiesToUnloadByRules = determineEntitiesToUnloadByRules();

        // Pass the entities to their rules to store them before unload
        tellRulesToStoreUnloadingEntities(entitiesToUnloadByRules);

        // Send events to them
        Collection<SimpleEntity> entitiesToUnload = entitiesToUnloadByRules.values();
        notifyEntitiesTheyAreBeingUnloaded(entitiesToUnload);

        // Unload the entities
        unloadTheEntities(entitiesToUnload);

        entityListeners.forEach(
                listener -> listener.entitiesModified(entitiesToUnload));

        int lastMaxId = maxId;

        // Load any new entities that became relevant
        Set<SimpleEntity> loadedEntities = loadNewlyRelevantEntities();

        entityListeners.forEach(
                listener -> listener.entitiesModified(loadedEntities));

        // Send events to them
        sendEventsToThem(loadedEntities, lastMaxId);

        entityRelevanceRules.forEach(EntityRelevanceRule::newRelevantEntitiesLoaded);
    }

    @Override
    public int getEntityId(EntityRef entityRef) {
        return ((EntityRefImpl) entityRef).entity.getEntityId();
    }

    @Override
    public String getEntityUniqueIdentifier(EntityRef entityRef) {
        return String.valueOf(getEntityId(entityRef));
    }

    private void sendEventsToThem(Set<SimpleEntity> loadedEntities, int createdIfIdGreaterThan) {
        for (SimpleEntity entity : loadedEntities) {
            Map<Class<? extends Component>, Component> components = new HashMap<>();
            for (Map.Entry<Class<? extends Component>, Component> originalComponents : entity.entityValues.entrySet()) {
                components.put(originalComponents.getKey(), internalComponentManager.copyComponentUnmodifiable(originalComponents.getValue(), false));
            }

            EntityRefImpl entityRef = new EntityRefImpl(entity, false);
            if (entity.getEntityId() <= createdIfIdGreaterThan) {
                entityRef.send(new AfterEntityLoaded(components));
            } else {
                entityRef.send(new AfterComponentAdded(components));
            }
        }
    }

    private Set<SimpleEntity> loadNewlyRelevantEntities() {
        Set<SimpleEntity> loadedEntities = new HashSet<>();
        for (EntityRelevanceRule entityRelevanceRule : entityRelevanceRules) {
            entityRelevanceRule.getNewRelevantEntities().forEach(
                    entityData -> {
                        int id = entityData.getEntityId();
                        if (id == 0)
                            id = ++maxId;
                        SimpleEntity entity = new SimpleEntity(internalComponentManager, id);
                        entityData.getComponents().forEach(
                                componentData -> {
                                    Class<? extends Component> componentClass = componentData.getComponentClass();
                                    Component component = internalComponentManager.createComponent(null, componentClass);
                                    componentData.getFields().entrySet().forEach(
                                            fieldNameAndValue -> internalComponentManager.setComponentFieldValue(component, fieldNameAndValue.getKey(), fieldNameAndValue.getValue()));
                                    entity.entityValues.put(componentClass, component);
                                });
                        entities.add(entity);
                        loadedEntities.add(entity);
                    });
        }
        return loadedEntities;
    }

    private void unloadTheEntities(Collection<SimpleEntity> entitiesToUnload) {
        entitiesToUnload.forEach(
                entity -> {
                    entity.exists = false;
                    entities.remove(entity);
                });
    }

    private void notifyEntitiesTheyAreBeingUnloaded(Collection<SimpleEntity> entitiesToUnload) {
        entitiesToUnload.forEach(
                entity -> {
                    Map<Class<? extends Component>, Component> components = new HashMap<>();
                    for (Map.Entry<Class<? extends Component>, Component> originalComponents : entity.entityValues.entrySet()) {
                        components.put(originalComponents.getKey(), internalComponentManager.copyComponentUnmodifiable(originalComponents.getValue(), false));
                    }

                    new EntityRefImpl(entity, false).send(new BeforeEntityUnloaded(components));
                });
    }

    private void tellRulesToStoreUnloadingEntities(Multimap<EntityRelevanceRule, SimpleEntity> entitiesToUnload) {
        for (Map.Entry<EntityRelevanceRule, Collection<SimpleEntity>> ruleEntities : entitiesToUnload.asMap().entrySet()) {
            EntityRelevanceRule rule = ruleEntities.getKey();
            rule.storeEntities(ruleEntities.getValue());
        }
    }

    private Multimap<EntityRelevanceRule, SimpleEntity> determineEntitiesToUnloadByRules() {
        Multimap<EntityRelevanceRule, SimpleEntity> entitiesToUnload = HashMultimap.create();
        for (EntityRelevanceRule entityRelevanceRule : entityRelevanceRules) {
            for (EntityRef entityRef : entityRelevanceRule.getNotRelevantEntities()) {
                entitiesToUnload.put(entityRelevanceRule, ((EntityRefImpl) entityRef).entity);
            }
        }
        return entitiesToUnload;
    }

    @Override
    public EntityRef createEntity() {
        SimpleEntity entity = new SimpleEntity(internalComponentManager, ++maxId);
        entities.add(entity);
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
    public EntityRef wrapEntity(SimpleEntity entity) {
        return new EntityRefImpl(entity, false);
    }

    @Override
    public void destroyEntity(EntityRef entityRef) {
        Collection<Class<? extends Component>> components = entityRef.listComponents();
        //noinspection unchecked
        entityRef.removeComponents(components.toArray(new Class[components.size()]));
        SimpleEntity underlyingEntity = ((EntityRefImpl) entityRef).entity;
        underlyingEntity.exists = false;
        entities.remove(underlyingEntity);
    }

    @Override
    public Iterable<EntityRef> getEntitiesWithComponents(Class<? extends Component> component, Class<? extends Component>... additionalComponents) {
        return Iterables.transform(Iterables.filter(entities,
                entity -> {
                    if (!entity.entityValues.containsKey(component))
                        return false;

                    for (Class<? extends Component> additionalComponent : additionalComponents) {
                        if (!entity.entityValues.containsKey(additionalComponent))
                            return false;
                    }

                    return true;
                }),
                entity -> new EntityRefImpl(entity, false));
    }

    private void sendEventToEntity(SimpleEntity entity, Event event) {
        entityEventListeners.forEach(entityListener -> entityListener.eventSent(new EntityRefImpl(entity, false), event));
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
