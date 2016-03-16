package com.gempukku.terasology.graphics.environment;

import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface ChunkMeshGeneratorCallback {
    boolean isNeighbourBlockCoveringSide(ChunkBlocks[] chunkSector, int x, int y, int z, BlockSide blockSide);
}
