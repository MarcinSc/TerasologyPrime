package com.gempukku.terasology.graphics.environment;

import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface ChunkMeshGeneratorCallback {
    boolean isNeighbourBlockCoveringSide(ChunkBlocks chunkBlocks, int x, int y, int z, BlockSide blockSide);
}
