package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
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
    public EntityRefAndCommonBlockId getBlockEntityAndBlockIdAt(String worldId, int x, int y, int z) {
        String commonBlockAt = getBlockIdAt(worldId, x, y, z);
        if (commonBlockAt == null)
            return null;

        for (EntityRef entityRef : entityManager.getEntitiesWithComponents(BlockComponent.class, LocationComponent.class)) {
            LocationComponent location = entityRef.getComponent(LocationComponent.class);
            if (location.getWorldId().equals(worldId) && location.getX() == x
                    && location.getY() == y && location.getZ() == z)
                return new EntityRefAndCommonBlockId(entityRef, commonBlockAt);
        }

        return new EntityRefAndCommonBlockId(null, commonBlockAt);
    }

    @Override
    public String getBlockIdAt(String worldId, int x, int y, int z) {
        return chunkBlocksProvider.getCommonBlockAt(worldId, x, y, z);
    }

    @Override
    public boolean hasChunk(String worldId, int x, int y, int z) {
        return chunkBlocksProvider.isChunkLoaded(worldId, x, y, z);
    }
}
