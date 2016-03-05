package com.gempukku.terasology.world.chunk;

public class ChunkBlocks {
    public enum Status {
        QUEUED, READY
    }

    public final String worldId;
    public final int x;
    public final int y;
    public final int z;
    private Status status;

    private String[] blocks;

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

    public void setBlocks(String[] blocks) {
        this.blocks = blocks;
    }

    public String[] getBlocks() {
        return blocks;
    }

    public String getCommonBlockAt(int xInChunk, int yInChunk, int zInChunk) {
        int index = getIndex(xInChunk, yInChunk, zInChunk);
        return blocks[index];
    }

    private int getIndex(int xInChunk, int yInChunk, int zInChunk) {
        return zInChunk + (ChunkSize.Z * yInChunk) + (ChunkSize.Z * ChunkSize.Y * xInChunk);
    }
}
