package com.gempukku.terasology.world.chunk;

public interface ChunkRelevanceRule {
    Iterable<ChunkLocation> getRelevantChunks();

    boolean isChunkRelevant(ChunkLocation chunk);
}
