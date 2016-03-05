package com.gempukku.secsy.entity;

/**
 * Class allowing to create/destroy entities, as well as register listeners to entities lifecycle.
 */
public interface EntityManager {
    EntityRef createEntity();

    EntityRef createNewEntityRef(EntityRef entityRef);

    boolean isSameEntity(EntityRef ref1, EntityRef ref2);

    void destroyEntity(EntityRef entityRef);

    Iterable<EntityRef> getEntitiesWithComponents(Class<? extends Component> component, Class<? extends Component>... additionalComponents);
}
