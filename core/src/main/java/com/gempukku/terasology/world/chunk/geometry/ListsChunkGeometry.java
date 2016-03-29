package com.gempukku.terasology.world.chunk.geometry;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;

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
        Iterable<Triangle>[] triangles = new Iterable[indicesPerTexture.length];
        for (int i = 0; i < indicesPerTexture.length; i++) {
            triangles[i] = trianglesForTexture(i);
        }

        return Iterables.concat(triangles);
    }

    private Iterable<Triangle> trianglesForTexture(int index) {
        short[] indices = indicesPerTexture[index];
        float[] vertices = verticesPerTexture[index];

        List<Triangle> triangles = new ArrayList<>(indices.length / 3);
        for (int i = 0; i < indices.length; i += 3) {
            int vertexFlag = Math.round(vertices[floatsPerVertex * indices[i] + 8]);
            if ((vertexFlag & BlockGeometryGenerator.DOES_NOT_PRODUCE_GEOMETRY) == 0) {

                int index1 = floatsPerVertex * indices[i];
                int index2 = floatsPerVertex * indices[i + 1];
                int index3 = floatsPerVertex * indices[i + 2];
                triangles.add(
                        new BasicTriangle(
                                vertices[index1 + 0], vertices[index1 + 1], vertices[index1 + 2],
                                vertices[index2 + 0], vertices[index2 + 1], vertices[index2 + 2],
                                vertices[index3 + 0], vertices[index3 + 1], vertices[index3 + 2],
                                vertices[index1 + 3], vertices[index1 + 4], vertices[index1 + 5]));
            }
        }

        return triangles;
    }
}
