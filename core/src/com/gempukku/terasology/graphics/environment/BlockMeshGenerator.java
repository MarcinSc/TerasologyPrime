package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface BlockMeshGenerator {
    void generateMeshForBlockFromAtlas(ChunkMeshGeneratorCallback callback, FloatArray vertices, ShortArray indices,
                                       Texture texture, ChunkBlocks chunkBlocks,
                                       int xInChunk, int yInChunk, int zInChunk);
}
