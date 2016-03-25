package com.gempukku.terasology.graphics.environment.mesh;

import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.terasology.graphics.environment.event.AfterChunkGeometryCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkGeometryRemoved;
import com.gempukku.terasology.world.chunk.IntLocationKey;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometryContainer;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometryManager;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = "generateChunkMeshes",
        shared = ChunkMeshManager.class)
public class ChunkMeshSystem implements ChunkMeshManager {
    @In
    private ChunkGeometryManager chunkGeometryManager;
    @In
    private ChunkMeshGenerator chunkMeshGenerator;

    private Map<IntLocationKey, ChunkMesh> meshesInWorld = new HashMap<>();

    @Override
    public ChunkMesh getChunkMesh(String worldId, int x, int y, int z) {
        return meshesInWorld.get(new IntLocationKey(worldId, x, y, z));
    }

    @ReceiveEvent
    public void chunkGeometryCreated(AfterChunkGeometryCreated event, EntityRef worldEntity) {
        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(event.worldId, event.x, event.y, event.z);
        Array<MeshPart> array = chunkMeshGenerator.generateMeshParts(chunkGeometryContainer.getChunkGeometry());
        ChunkMesh chunkMesh = new ChunkMesh(event.worldId, event.x, event.y, event.z);
        chunkMesh.setMeshParts(array);
        meshesInWorld.put(new IntLocationKey(chunkMesh), chunkMesh);
        worldEntity.send(new AfterChunkMeshCreated(event.worldId, event.x, event.y, event.z));
    }

    @ReceiveEvent
    public void chunkGeometryRemoved(BeforeChunkGeometryRemoved event, EntityRef worldEntity) {
        ChunkMesh chunkMesh = getChunkMesh(event.worldId, event.x, event.y, event.z);
        worldEntity.send(new BeforeChunkMeshRemoved(event.worldId, event.x, event.y, event.z));
        meshesInWorld.remove(new IntLocationKey(chunkMesh));
        for (MeshPart meshPart : chunkMesh.getMeshParts()) {
            if (meshPart != null && meshPart.mesh != null) {
                meshPart.mesh.dispose();
            }
        }
    }
}
