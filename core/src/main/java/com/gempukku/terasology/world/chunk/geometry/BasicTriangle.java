package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.math.Vector3;

public class BasicTriangle implements Triangle {
    private int blockX;
    private int blockY;
    private int blockZ;
    private float x1;
    private float y1;
    private float z1;
    private float x2;
    private float y2;
    private float z2;
    private float x3;
    private float y3;
    private float z3;
    private float normalX;
    private float normalY;
    private float normalZ;

    public BasicTriangle(
            int blockX, int blockY, int blockZ,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float normalX, float normalY, float normalZ) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.x3 = x3;
        this.y3 = y3;
        this.z3 = z3;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }

    @Override
    public void getBlockVertices(Vector3 blockLocation, Vector3 vertex1, Vector3 vertex2, Vector3 vertex3, Vector3 normal) {
        if (blockLocation != null)
            blockLocation.set(blockX, blockY, blockZ);
        vertex1.set(x1, y1, z1);
        vertex2.set(x2, y2, z2);
        vertex3.set(x3, y3, z3);
        if (normal != null)
        normal.set(normalX, normalY, normalZ);
    }
}
