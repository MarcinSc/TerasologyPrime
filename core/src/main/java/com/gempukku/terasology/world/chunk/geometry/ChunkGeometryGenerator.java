package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.graphics.Texture;

import java.util.List;

public interface ChunkGeometryGenerator<T extends ChunkGeometry> {
    boolean canPrepareChunkData(String worldId, int x, int y, int z);

    /**
     * This method will be called off the main thread, as it is usually time consuming to generate mesh lists.
     *
     * @param textures
     * @param worldId
     * @param x
     * @param y
     * @param z
     * @return
     */
    // Would be nice to get rid of the "textures" here
    T prepareChunkGeometryOffThread(List<Texture> textures, String worldId, int x, int y, int z);
}
