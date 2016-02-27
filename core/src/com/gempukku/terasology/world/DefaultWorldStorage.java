package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.prefab.PrefabData;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.DefaultComponentData;
import com.gempukku.terasology.world.chunk.DefaultEntityData;
import com.gempukku.terasology.world.component.BlockComponent;

@RegisterSystem(
        shared = WorldStorage.class)
public class DefaultWorldStorage implements WorldStorage {
    @In
    private EntityManager entityManager;
    @In
    private ChunkBlocksProvider chunkBlocksProvider;
    @In
    private PrefabManager prefabManager;
    @In
    private CommonBlockManager commonBlockManager;

    @Override
    public EntityRef getBlockEntityAt(String worldId, int x, int y, int z) {
        EntityRefOrCommonBlockId blockEntityOrBlockIdAt = getBlockEntityOrBlockIdAt(worldId, x, y, z);
        if (blockEntityOrBlockIdAt.entityRef != null)
            return blockEntityOrBlockIdAt.entityRef;

        if (blockEntityOrBlockIdAt.commonBlockId != null) {
            PrefabData prefabData = commonBlockManager.getCommonBlockById(blockEntityOrBlockIdAt.commonBlockId);

            DefaultEntityData entityData = new DefaultEntityData(prefabManager.convertToEntityData(prefabData));
            DefaultComponentData locationComponentData = new DefaultComponentData(LocationComponent.class);
            locationComponentData.addField("worldId", worldId);
            locationComponentData.addField("x", (float) x);
            locationComponentData.addField("y", (float) y);
            locationComponentData.addField("z", (float) z);

            entityData.addComponent(locationComponentData);
            return entityManager.createNewTemporaryEntity(entityData);
        }

        return null;
    }

    @Override
    public EntityRefOrCommonBlockId getBlockEntityOrBlockIdAt(String worldId, int x, int y, int z) {
        for (EntityRef entityRef : entityManager.getEntitiesWithComponents(BlockComponent.class, LocationComponent.class)) {
            LocationComponent location = entityRef.getComponent(LocationComponent.class);
            if (location.getWorldId().equals(worldId) && location.getX() == x
                    && location.getY() == y && location.getZ() == z)
                return new EntityRefOrCommonBlockId(entityRef, null);
        }

        String commonBlockAt = chunkBlocksProvider.getCommonBlockAt(worldId, x, y, z);
        if (commonBlockAt != null) {
            return new EntityRefOrCommonBlockId(null, commonBlockAt);
        }

        return null;
    }

    @Override
    public boolean hasChunk(String worldId, int x, int y, int z) {
        return chunkBlocksProvider.isChunkLoaded(worldId, x, y, z);
    }
}
