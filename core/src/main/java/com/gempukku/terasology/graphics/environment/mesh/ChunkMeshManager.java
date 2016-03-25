package com.gempukku.terasology.graphics.environment.mesh;

public interface ChunkMeshManager {
    ChunkMesh getChunkMesh(String worldId, int x, int y, int z);
}
