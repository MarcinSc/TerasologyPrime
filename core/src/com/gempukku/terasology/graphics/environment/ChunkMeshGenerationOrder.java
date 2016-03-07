package com.gempukku.terasology.graphics.environment;

import java.util.Collection;

public interface ChunkMeshGenerationOrder {
    ChunkMesh getChunkMeshToProcess(Collection<ChunkMesh> chunkMeshes);
}
