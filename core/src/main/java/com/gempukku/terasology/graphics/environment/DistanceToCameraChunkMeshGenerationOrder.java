package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Collection;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = ChunkMeshGenerationOrder.class)
public class DistanceToCameraChunkMeshGenerationOrder implements ChunkMeshGenerationOrder {
    private Vector3 cameraPosition;

    @ReceiveEvent
    public void cameraAdded(AfterComponentAdded event, EntityRef entity, CameraComponent camera, LocationComponent location) {
        cameraPosition = new Vector3(location.getX(), location.getY(), location.getZ());
    }

    @ReceiveEvent
    public void cameraPositionChanged(AfterComponentUpdated event, EntityRef entity, CameraComponent camera, LocationComponent location) {
        cameraPosition.set(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public ChunkMesh getChunkMeshToProcess(Collection<ChunkMesh> chunkMeshes) {
        ChunkMesh result = null;
        float minDistance = Float.MAX_VALUE;
        for (ChunkMesh chunkMesh : chunkMeshes) {
            if (chunkMesh.getStatus().canOfflineProcess()) {
                float distance = cameraPosition.dst(
                        (chunkMesh.x + 0.5f) * ChunkSize.X,
                        (chunkMesh.y + 0.5f) * ChunkSize.Y,
                        (chunkMesh.z + 0.5f) * ChunkSize.Z);
                if (distance < minDistance) {
                    minDistance = distance;
                    result = chunkMesh;
                }
            }
        }

        return result;
    }
}
