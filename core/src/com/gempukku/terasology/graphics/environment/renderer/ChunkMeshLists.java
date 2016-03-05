package com.gempukku.terasology.graphics.environment.renderer;

import java.util.List;

public class ChunkMeshLists {
    public final List<float[]> verticesPerTexture;
    public final List<short[]> indicesPerTexture;

    public ChunkMeshLists(List<float[]> verticesPerTexture, List<short[]> indicesPerTexture) {
        this.verticesPerTexture = verticesPerTexture;
        this.indicesPerTexture = indicesPerTexture;
    }
}
