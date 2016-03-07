package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.world.component.MultiverseComponent;
import com.gempukku.terasology.world.component.WorldComponent;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = MultiverseManager.class)
public class DefaultMultiverseManager implements MultiverseManager {
    @In
    private EntityManager entityManager;

    @Override
    public EntityRef createWorld(String worldId) {
        if (!hasMultiverse()) {
            createMultiverseEntity();
        }
        if (hasWorld(worldId))
            throw new IllegalStateException("This multiverse already has a world with id: " + worldId);
        EntityRef entity = entityManager.createEntity();
        WorldComponent component = entity.createComponent(WorldComponent.class);
        component.setWorldId(worldId);
        entity.saveComponents(component);
        return entity;
    }

    private void createMultiverseEntity() {
        EntityRef multiverseEntity = entityManager.createEntity();
        MultiverseComponent multiverseComponent = multiverseEntity.createComponent(MultiverseComponent.class);
        multiverseEntity.saveComponents(multiverseComponent);
    }

    @Override
    public EntityRef getWorldEntity(String worldId) {
        for (EntityRef entityRef : entityManager.getEntitiesWithComponents(WorldComponent.class)) {
            WorldComponent component = entityRef.getComponent(WorldComponent.class);
            if (component.getWorldId().equals(worldId))
                return entityRef;
        }

        return null;
    }

    @Override
    public boolean hasWorld(String worldId) {
        return getWorldEntity(worldId) != null;
    }

    private boolean hasMultiverse() {
        return entityManager.getEntitiesWithComponents(MultiverseComponent.class).iterator().hasNext();
    }
}
