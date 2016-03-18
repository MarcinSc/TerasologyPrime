package com.gempukku.terasology.world;

import com.gempukku.secsy.entity.io.EntityData;

public interface CommonBlockManager {
    EntityData getCommonBlockById(short id);

    short getCommonBlockId(String commonBlockId);

    int getCommonBlockCount();
}
