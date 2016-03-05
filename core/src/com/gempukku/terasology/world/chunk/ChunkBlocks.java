package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.EntityRef;

public class ChunkBlocks {
    public enum Status {
        QUEUED, READY
    }

    public final String worldId;
    public final int x;
    public final int y;
    public final int z;
    private Status status;

    private EntityRef chunkEntity;

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

    public void setChunkEntity(EntityRef chunkEntity) {
        this.chunkEntity = chunkEntity;
    }

    public String getCommonBlockAt(int xInChunk, int yInChunk, int zInChunk) {
        int index = getIndex(xInChunk, yInChunk, zInChunk);
        String blockId = chunkEntity.getComponent(ChunkComponent.class).getChunkBlocks()[index];
        if (blockId == null) {
            System.out.println("HM?!");
        }
        return blockId;
    }

    private int getIndex(int xInChunk, int yInChunk, int zInChunk) {
        return zInChunk + (ChunkSize.Z * yInChunk) + (ChunkSize.Z * ChunkSize.Y * xInChunk);
    }
}
