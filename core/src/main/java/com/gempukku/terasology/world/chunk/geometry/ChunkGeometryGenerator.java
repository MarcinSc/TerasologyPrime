package com.gempukku.terasology.world.chunk.geometry;

public interface ChunkGeometryGenerator<T extends ChunkGeometry> {
    boolean canPrepareChunkData(String worldId, int x, int y, int z);

    /**
     * This method will be called off the main thread, as it is usually time consuming to generate mesh lists.
     *
     * @param worldId
     * @param x
     * @param y
     * @param z
     * @return
     */
    T prepareChunkGeometryOffThread(String worldId, int x, int y, int z);
}
