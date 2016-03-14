package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.blockEntity.BlockEntityManager;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;

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
    @In
    private BlockEntityManager blockEntityManager;

    @Override
    public EntityRefAndCommonBlockId getBlockEntityAndBlockIdAt(String worldId, int x, int y, int z) {
        short commonBlockAt = getBlockIdAt(worldId, x, y, z);
        if (commonBlockAt == -1)
            return null;

        EntityRef entity = blockEntityManager.getBlockEntityAt(worldId, x, y, z);

        return new EntityRefAndCommonBlockId(entity, commonBlockAt);
    }

    @Override
    public short getBlockIdAt(String worldId, int x, int y, int z) {
        return chunkBlocksProvider.getCommonBlockAt(worldId, x, y, z);
    }

    @Override
    public boolean hasChunk(String worldId, int x, int y, int z) {
        return chunkBlocksProvider.isChunkLoaded(worldId, x, y, z);
    }
}
