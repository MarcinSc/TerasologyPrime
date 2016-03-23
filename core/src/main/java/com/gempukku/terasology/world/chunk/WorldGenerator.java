package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.io.EntityData;

public interface WorldGenerator {
    EntityData createMultiverseEntity();

    EntityData createWorldEntity(String worldId);

    Iterable<EntityDataOrCommonBlock> generateChunk(String worldId, int x, int y, int z);

    class EntityDataOrCommonBlock {
        public final EntityData entityData;
        public final short commonBlock;

        public static EntityDataOrCommonBlock commonBlock(short commonBlock) {
            return new EntityDataOrCommonBlock(commonBlock, null);
        }

        public static EntityDataOrCommonBlock entityData(short commonBlock, EntityData entityData) {
            return new EntityDataOrCommonBlock(commonBlock, entityData);
        }

        private EntityDataOrCommonBlock(short commonBlock, EntityData entityData) {
            this.commonBlock = commonBlock;
            this.entityData = entityData;
        }
    }
}
