package com.gempukku.terasology;

import com.gempukku.secsy.entity.EntityRef;

public interface PlayerManager {
    EntityRef createPlayer(String id);
    EntityRef getPlayer(String id);
}
