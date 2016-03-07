package com.gempukku.terasology.graphics.environment;

public class ChunkMeshLists {
    public final int floatsPerVertex;
    public final float[][] verticesPerTexture;
    public final short[][] indicesPerTexture;

    public ChunkMeshLists(int floatsPerVertex, float[][] verticesPerTexture, short[][] indicesPerTexture) {
        this.floatsPerVertex = floatsPerVertex;
        this.verticesPerTexture = verticesPerTexture;
        this.indicesPerTexture = indicesPerTexture;
    }
}
