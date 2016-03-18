package com.gempukku.terasology.world.chunk;

public interface ChunkBlocksProvider {
    short getCommonBlockAt(String worldId, int x, int y, int z);
    boolean isChunkLoaded(String worldId, int x, int y, int z);

    ChunkBlocks getChunkBlocks(String worldId, int x, int y, int z);
}
