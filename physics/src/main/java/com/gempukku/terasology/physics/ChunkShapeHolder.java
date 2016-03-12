package com.gempukku.terasology.physics;

import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;

public class ChunkShapeHolder {
    public final int x;
    public final int y;
    public final int z;
    public final btCollisionShape chunkShape;

    public ChunkShapeHolder(btCollisionShape chunkShape, int x, int y, int z) {
        this.chunkShape = chunkShape;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
