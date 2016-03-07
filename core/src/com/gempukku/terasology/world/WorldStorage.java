package com.gempukku.terasology.world;

import com.gempukku.secsy.entity.EntityRef;

public interface WorldStorage {
    EntityRefAndCommonBlockId getBlockEntityAndBlockIdAt(String worldId, int x, int y, int z);

    short getBlockIdAt(String worldId, int x, int y, int z);

    boolean hasChunk(String worldId, int x, int y, int z);

    class EntityRefAndCommonBlockId {
        public final EntityRef entityRef;
        public final short commonBlockId;

        public EntityRefAndCommonBlockId(EntityRef entityRef, short commonBlockId) {
            this.entityRef = entityRef;
            this.commonBlockId = commonBlockId;
        }
    }
}
