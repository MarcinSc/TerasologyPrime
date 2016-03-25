package com.gempukku.terasology.world.chunk.geometry;

public class ListsChunkGeometry implements ChunkGeometry {
    public final int floatsPerVertex;
    public final float[][] verticesPerTexture;
    public final short[][] indicesPerTexture;

    public ListsChunkGeometry(int floatsPerVertex, float[][] verticesPerTexture, short[][] indicesPerTexture) {
        this.floatsPerVertex = floatsPerVertex;
        this.verticesPerTexture = verticesPerTexture;
        this.indicesPerTexture = indicesPerTexture;
    }

    @Override
    public Iterable<Triangle> getTriangles() {
        return null;
    }
}
