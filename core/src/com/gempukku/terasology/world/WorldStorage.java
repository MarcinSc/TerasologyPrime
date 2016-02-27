package com.gempukku.terasology.world;

import com.gempukku.secsy.entity.EntityRef;

public interface WorldStorage {
    EntityRef getBlockEntityAt(String worldId, int x, int y, int z);

    EntityRefOrCommonBlockId getBlockEntityOrBlockIdAt(String worldId, int x, int y, int z);

    boolean hasChunk(String worldId, int x, int y, int z);

    class EntityRefOrCommonBlockId {
        public final EntityRef entityRef;
        public final String commonBlockId;

        public EntityRefOrCommonBlockId(EntityRef entityRef, String commonBlockId) {
            this.entityRef = entityRef;
            this.commonBlockId = commonBlockId;
        }
    }
}
