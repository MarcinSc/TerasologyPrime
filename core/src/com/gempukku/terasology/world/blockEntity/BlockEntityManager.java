package com.gempukku.terasology.world.blockEntity;

import com.gempukku.secsy.entity.EntityRef;

public interface BlockEntityManager {
    EntityRef getBlockEntityAt(String worldId, int x, int y, int z);
}
