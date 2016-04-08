package com.gempukku.terasology.physics;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Predicate;
import com.gempukku.secsy.entity.io.EntityData;

public interface PhysicsEngine {
    Collision getFirstCollision(String worldId, Ray ray, float distance, Predicate<EntityData> entityDataPredicate);

    interface Collision {
        void getBlock(Vector3 vector);

        void getCollision(Vector3 vector);

        void getCollisionNormal(Vector3 vector);
    }
}
