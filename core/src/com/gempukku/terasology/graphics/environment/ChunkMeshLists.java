package com.gempukku.terasology.graphics.environment;

import java.util.List;

public class ChunkMeshLists {
    public final int floatsPerVertex;
    public final List<float[]> verticesPerTexture;
    public final List<short[]> indicesPerTexture;

    public ChunkMeshLists(int floatsPerVertex, List<float[]> verticesPerTexture, List<short[]> indicesPerTexture) {
        this.floatsPerVertex = floatsPerVertex;
        this.verticesPerTexture = verticesPerTexture;
        this.indicesPerTexture = indicesPerTexture;
    }
}
