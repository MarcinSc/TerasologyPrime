package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Texture;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface BlockMeshGenerator {
    int INFLUENCED_BY_WIND = 1;

    void generateMeshForBlockFromAtlas(ChunkMeshGeneratorCallback callback, VertexOutput vertexOutput,
                                       Texture texture, ChunkBlocks chunkBlocks,
                                       int xInChunk, int yInChunk, int zInChunk);

    interface VertexOutput {
        void setPosition(float x, float y, float z);

        void setNormal(float x, float y, float z);

        void setTextureCoordinate(float x, float y);

        void setFlag(int flag);

        short finishVertex();

        void addVertexIndex(short vertexIndex);
    }
}
