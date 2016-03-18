package com.gempukku.terasology.world;

import com.gempukku.secsy.entity.EntityRef;

public interface MultiverseManager {
    EntityRef createWorld(String worldId);

    EntityRef getWorldEntity(String worldId);

    EntityRef getMultiverseEntity();

    boolean hasWorld(String worldId);
}
