package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;

import java.util.List;

public interface ChunkMeshGenerator<T> {
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
    T prepareChunkDataOffThread(List<Texture> textures, String worldId, int x, int y, int z);

    /**
     * This method will be called from the main thread to generate MeshParts based on the object created
     * by the class.
     *
     * @param preparedData
     * @return
     */
    Array<MeshPart> generateMeshParts(T preparedData);
}
