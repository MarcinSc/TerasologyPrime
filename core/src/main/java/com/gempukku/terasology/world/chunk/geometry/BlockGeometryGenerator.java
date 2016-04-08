package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.graphics.Texture;
import com.gempukku.terasology.graphics.environment.mesh.ChunkMeshGeneratorCallback;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface BlockGeometryGenerator {
    // Binary flags that can be set to a vertex
    int DOES_NOT_PRODUCE_GEOMETRY = 1;
    int MOVING_ON_WIND = 2;

    void generateGeometryForBlockFromAtlas(ChunkMeshGeneratorCallback callback, BlockVertexOutput vertexOutput,
                                           Texture texture, ChunkBlocks chunkBlocks,
                                           int xInChunk, int yInChunk, int zInChunk);

    interface BlockVertexOutput {
        void setBlock(int x, int y, int z);

        void setPosition(float x, float y, float z);

        void setNormal(float x, float y, float z);

        void setTextureCoordinate(float x, float y);

        void setFlag(int flag);

        short finishVertex();

        void addVertexIndex(short vertexIndex);
    }
}
