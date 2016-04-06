package com.gempukku.terasology.faction;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.entity.EntityRef;

public interface FactionManager {
    Iterable<EntityRef> findClosestEnemies(String faction, Vector3 position, float distance);
}
