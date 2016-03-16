package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Texture;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface BlockMeshGenerator {
    void generateMeshForBlockFromAtlas(ChunkMeshGeneratorCallback callback, VertexOutput vertexOutput,
                                       Texture texture, ChunkBlocks chunkBlocks,
                                       int xInChunk, int yInChunk, int zInChunk);

    interface VertexOutput {
        void setPosition(float x, float y, float z);

        void setNormal(float x, float y, float z);

        void setTextureCoordinate(float x, float y);

        short finishVertex();

        void addVertexIndex(short vertexIndex);
    }
}
