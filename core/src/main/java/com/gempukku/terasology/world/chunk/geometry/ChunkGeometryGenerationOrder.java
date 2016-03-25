package com.gempukku.terasology.world.chunk.geometry;

import java.util.Collection;

public interface ChunkGeometryGenerationOrder {
    ChunkGeometryContainer getChunkMeshToProcess(Collection<ChunkGeometryContainer> chunkGeometryContainers);
}
