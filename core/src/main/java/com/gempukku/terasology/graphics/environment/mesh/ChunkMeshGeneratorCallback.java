package com.gempukku.terasology.graphics.environment.mesh;

import com.gempukku.terasology.graphics.shape.BlockSide;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface ChunkMeshGeneratorCallback {
    boolean isNeighbourBlockCoveringSide(ChunkBlocks[] chunkSector, int x, int y, int z, BlockSide blockSide);
}
