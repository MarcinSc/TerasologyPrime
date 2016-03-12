package com.gempukku.terasology.physics;

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Disposable;
import com.gempukku.secsy.entity.EntityRef;

public class MovingObjectHolder implements Disposable {
    public final int userValue;
    public final btCollisionShape collisionShape;
    public final btRigidBody rigidBody;
    public final EntityRef entityRef;

    public MovingObjectHolder(int userValue, EntityRef entityRef, btCollisionShape collisionShape, float mass) {
        this.userValue = userValue;
        this.entityRef = entityRef;
        this.collisionShape = collisionShape;
        rigidBody = new btRigidBody(mass, null, collisionShape);
        rigidBody.setUserValue(userValue);
        rigidBody.setCollisionFlags(rigidBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
    }

    public void dispose() {
        collisionShape.dispose();
        rigidBody.dispose();
    }
}
