package com.gempukku.terasology.world.chunk.geometry;

import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;

import java.util.Collection;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = ChunkGeometryGenerationOrder.class)
public class FirstChunkGeometryGenerationOrder implements ChunkGeometryGenerationOrder {
    @Override
    public ChunkGeometryContainer getChunkMeshToProcess(Collection<ChunkGeometryContainer> chunkGeometryContainers) {
        for (ChunkGeometryContainer chunkGeometryContainer : chunkGeometryContainers) {
            if (chunkGeometryContainer.getStatus() == ChunkGeometryContainer.Status.QUEUED_FOR_GENERATOR)
                return chunkGeometryContainer;
        }
        return null;
    }
}
