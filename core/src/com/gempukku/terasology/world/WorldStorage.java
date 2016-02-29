package com.gempukku.terasology.world;

import com.gempukku.secsy.entity.EntityRef;

public interface WorldStorage {
    EntityRefAndCommonBlockId getBlockEntityAndBlockIdAt(String worldId, int x, int y, int z);

    String getBlockIdAt(String worldId, int x, int y, int z);

    boolean hasChunk(String worldId, int x, int y, int z);

    class EntityRefAndCommonBlockId {
        public final EntityRef entityRef;
        public final String commonBlockId;

        public EntityRefAndCommonBlockId(EntityRef entityRef, String commonBlockId) {
            this.entityRef = entityRef;
            this.commonBlockId = commonBlockId;
        }
    }
}
