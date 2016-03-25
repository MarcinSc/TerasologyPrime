package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.graphics.Texture;
import com.gempukku.terasology.graphics.environment.mesh.ChunkMeshGeneratorCallback;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

public interface BlockGeometryGenerator {
    int INFLUENCED_BY_WIND = 1;

    void generateGeometryForBlockFromAtlas(ChunkMeshGeneratorCallback callback, VertexOutput vertexOutput,
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
