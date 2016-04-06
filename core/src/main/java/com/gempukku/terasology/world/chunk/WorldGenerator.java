package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.secsy.entity.io.StoredEntityData;

public interface WorldGenerator {
    EntityData createMultiverseEntity();

    Iterable<EntityData> createStartingEntities();

    EntityData createWorldEntity(String worldId);

    Iterable<StoredEntityData> generateChunk(String worldId, int x, int y, int z);
}
