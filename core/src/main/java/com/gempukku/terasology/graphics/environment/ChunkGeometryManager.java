package com.gempukku.terasology.graphics.environment;

public interface ChunkGeometryManager {
    ChunkGeometryContainer getChunkGeometry(String worldId, int x, int y, int z);
}
