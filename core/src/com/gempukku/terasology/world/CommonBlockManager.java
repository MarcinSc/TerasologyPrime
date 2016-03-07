package com.gempukku.terasology.world;

import com.gempukku.terasology.prefab.PrefabData;

public interface CommonBlockManager {
    PrefabData getCommonBlockById(short id);

    short getCommonBlockId(String commonBlockId);

    int getCommonBlockCount();
}
