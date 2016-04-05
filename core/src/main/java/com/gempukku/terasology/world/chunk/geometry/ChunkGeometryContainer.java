package com.gempukku.terasology.world.chunk.geometry;

import com.gempukku.terasology.world.chunk.ChunkLocation;

public class ChunkGeometryContainer implements ChunkLocation {
    public enum Status {
        // Status when first created, before checking if geometry can be generated by generator
        NOT_READY,
        // Queue for generating geometry - set when we know it can be generated
        QUEUED_FOR_GENERATOR,
        // Set when an off-thread picks up the chunk to generate objects required
        GENERATING,
        // Set when an off-thread finishes to generate objects required
        GENERATED,
        // Set when an event is sent about the status
        READY,
        // Set when this chunk geometry is no longer needed
        DISPOSED;
    }

    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    private volatile Status status;

    private volatile ChunkGeometry chunkGeometry;

    public ChunkGeometryContainer(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        status = Status.NOT_READY;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ChunkGeometry getChunkGeometry() {
        return chunkGeometry;
    }

    public void setChunkGeometry(ChunkGeometry chunkGeometry) {
        this.chunkGeometry = chunkGeometry;
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