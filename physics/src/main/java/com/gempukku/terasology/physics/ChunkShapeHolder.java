package com.gempukku.terasology.physics;

import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;

public class ChunkShapeHolder {
    public final int x;
    public final int y;
    public final int z;
    public final btCompoundShape chunkShape;

    public ChunkShapeHolder(btCompoundShape chunkShape, int x, int y, int z) {
        this.chunkShape = chunkShape;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
