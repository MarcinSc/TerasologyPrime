package com.gempukku.terasology.graphics.environment;

import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;

import java.util.Collection;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = ChunkMeshGenerationOrder.class)
public class FirstChunkMeshGenerationOrder implements ChunkMeshGenerationOrder {
    @Override
    public ChunkMesh getChunkMeshToProcess(Collection<ChunkMesh> chunkMeshes) {
        for (ChunkMesh chunkMesh : chunkMeshes) {
            if (chunkMesh.getStatus() == ChunkMesh.Status.QUEUED_FOR_GENERATOR)
                return chunkMesh;
        }
        return null;
    }
}
