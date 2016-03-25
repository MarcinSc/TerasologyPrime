package com.gempukku.terasology.world.chunk.geometry;

public interface ChunkGeometryManager {
    ChunkGeometryContainer getChunkGeometry(String worldId, int x, int y, int z);
}
