package com.gempukku.terasology.graphics.environment;

import java.util.Collection;

public interface ChunkGeometryGenerationOrder {
    ChunkGeometryContainer getChunkMeshToProcess(Collection<ChunkGeometryContainer> chunkGeometryContainers);
}
