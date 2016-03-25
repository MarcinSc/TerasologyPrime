package com.gempukku.terasology.graphics.environment.mesh;

import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometry;

public interface ChunkMeshGenerator<T extends ChunkGeometry> {
    /**
     * This method will be called from the main thread to generate MeshParts based on the chunk geometry
     * by the class.
     *
     * @param chunkGeometry
     * @return
     */
    Array<MeshPart> generateMeshParts(T chunkGeometry);
}
