package com.gempukku.terasology.world;

import com.gempukku.terasology.world.chunk.ChunkSize;

public class WorldBlock {
    private int x;
    private int y;
    private int z;

    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(float x, float y, float z) {
        this.x = Math.round(x);
        this.y = Math.round(y);
        this.z = Math.round(z);
    }

    public int getChunkX() {
        return (int) Math.floor(1f * x / ChunkSize.X);
    }

    public int getChunkY() {
        return (int) Math.floor(1f * y / ChunkSize.Y);
    }

    public int getChunkZ() {
        return (int) Math.floor(1f * z / ChunkSize.Z);
    }

    public int getInChunkX() {
        return x - getChunkX() * ChunkSize.X;
    }

    public int getInChunkY() {
        return y - getChunkY() * ChunkSize.Y;
    }

    public int getInChunkZ() {
        return z - getChunkZ() * ChunkSize.Z;
    }
}
