package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Collection;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = ChunkGeometryGenerationOrder.class)
public class DistanceToPlayerChunkGeometryGenerationOrder implements ChunkGeometryGenerationOrder {
    private Vector3 cameraPosition;

    @ReceiveEvent
    public void cameraAdded(AfterComponentAdded event, EntityRef entity, ClientComponent client, LocationComponent location) {
        cameraPosition = new Vector3(location.getX(), location.getY(), location.getZ());
    }

    @ReceiveEvent
    public void cameraPositionChanged(AfterComponentUpdated event, EntityRef entity, LocationComponent location) {
        if (entity.hasComponent(ClientComponent.class)) {
            cameraPosition.set(location.getX(), location.getY(), location.getZ());
        }
    }

    @Override
    public ChunkGeometryContainer getChunkMeshToProcess(Collection<ChunkGeometryContainer> chunkGeometryContainers) {
        ChunkGeometryContainer result = null;
        float minDistance = Float.MAX_VALUE;
        for (ChunkGeometryContainer chunkGeometryContainer : chunkGeometryContainers) {
            if (chunkGeometryContainer.getStatus() == ChunkGeometryContainer.Status.QUEUED_FOR_GENERATOR) {
                float distance = cameraPosition.dst(
                        (chunkGeometryContainer.x + 0.5f) * ChunkSize.X,
                        (chunkGeometryContainer.y + 0.5f) * ChunkSize.Y,
                        (chunkGeometryContainer.z + 0.5f) * ChunkSize.Z);
                if (distance < minDistance) {
                    minDistance = distance;
                    result = chunkGeometryContainer;
                }
            }
        }

        return result;
    }
}
