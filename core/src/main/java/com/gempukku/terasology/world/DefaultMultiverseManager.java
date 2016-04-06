package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.world.chunk.WorldGenerator;
import com.gempukku.terasology.world.component.MultiverseComponent;
import com.gempukku.terasology.world.component.WorldComponent;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = MultiverseManager.class)
public class DefaultMultiverseManager implements MultiverseManager {
    @In
    private EntityManager entityManager;
    @In
    private WorldGenerator worldGenerator;

    @Override
    public EntityRef createWorld(String worldId) {
        if (!hasMultiverse()) {
            createMultiverseEntity();
        }
        if (hasWorld(worldId))
            throw new IllegalStateException("This multiverse already has a world with id: " + worldId);
        EntityData worldData = worldGenerator.createWorldEntity(worldId);
        return entityManager.createEntity(worldData);
    }

    private void createMultiverseEntity() {
        EntityData multiverseData = worldGenerator.createMultiverseEntity();
        entityManager.createEntity(multiverseData);

        for (EntityData entityData : worldGenerator.createStartingEntities()) {
            entityManager.createEntity(entityData);
        }
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
    public EntityRef getMultiverseEntity() {
        for (EntityRef entityRef : entityManager.getEntitiesWithComponents(MultiverseComponent.class)) {
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
