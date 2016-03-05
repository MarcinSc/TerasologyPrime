package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.io.EntityData;

public interface ChunkGenerator {
    Iterable<EntityDataOrCommonBlock> generateChunk(String worldId, int x, int y, int z);

    class EntityDataOrCommonBlock {
        public final EntityData entityData;
        public final String commonBlock;

        public static EntityDataOrCommonBlock commonBlock(String commonBlock) {
            return new EntityDataOrCommonBlock(commonBlock, null);
        }

        public static EntityDataOrCommonBlock entityData(String commonBlock, EntityData entityData) {
            return new EntityDataOrCommonBlock(commonBlock, entityData);
        }

        private EntityDataOrCommonBlock(String commonBlock, EntityData entityData) {
            this.commonBlock = commonBlock;
            this.entityData = entityData;
        }
    }
}
