package com.gempukku.terasology.world.chunk;

public class ChunkBlocks implements ChunkLocation {
    public enum Status {
        QUEUED, GENERATING, READY
    }

    public final String worldId;
    public final int x;
    public final int y;
    public final int z;
    private volatile Status status;

    private short[] blocks;

    public ChunkBlocks(Status status, String worldId, int x, int y, int z) {
        this.status = status;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setBlocks(short[] blocks) {
        this.blocks = blocks;
    }

    public short[] getBlocks() {
        return blocks;
    }

    public short getCommonBlockAt(int xInChunk, int yInChunk, int zInChunk) {
        int index = getIndex(xInChunk, yInChunk, zInChunk);
        return blocks[index];
    }

    private int getIndex(int xInChunk, int yInChunk, int zInChunk) {
        return zInChunk + (ChunkSize.Z * yInChunk) + (ChunkSize.Z * ChunkSize.Y * xInChunk);
    }

    @Override
    public String getWorldId() {
        return worldId;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }
}
