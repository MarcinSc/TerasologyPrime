package com.gempukku.terasology.communication;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.context.util.PriorityCollection;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.network.server.ClientConnectedEvent;
import com.gempukku.secsy.entity.network.server.ClientEntityRelevanceRule;
import com.gempukku.secsy.entity.network.server.ClientEntityRelevancyRuleListener;
import com.gempukku.secsy.entity.network.server.ClientManager;
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.chunk.ChunkComponent;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.component.BlockComponent;
import com.gempukku.terasology.world.component.ClientComponent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class ClientReceivesBlocksAroundIt implements ClientEntityRelevanceRule, LifeCycleSystem {
    @In
    private EntityManager entityManager;
    @In
    private ClientManager clientManager;

    private PriorityCollection<ClientEntityRelevancyRuleListener> listeners = new PriorityCollection<>();

    @Override
    public void addClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void initialize() {
        clientManager.addClientEntityRelevanceRule(this);
    }

    private WorldBlock worldBlock = new WorldBlock();

    @Override
    public boolean isEntityRelevantForClient(EntityRef clientEntity, EntityRef entity) {
        if (entity.hasComponent(BlockComponent.class)) {
            LocationComponent location = clientEntity.getComponent(LocationComponent.class);
            LocationComponent blockLocation = entity.getComponent(LocationComponent.class);

            // If client is in different world
            if (!location.getWorldId().equals(blockLocation.getWorldId()))
                return false;

            ClientComponent client = clientEntity.getComponent(ClientComponent.class);

            worldBlock.set(FastMath.floor(location.getX()), FastMath.floor(location.getY()), FastMath.floor(location.getZ()));
            int playerChunkX = worldBlock.getChunkX();
            int playerChunkY = worldBlock.getChunkY();
            int playerChunkZ = worldBlock.getChunkZ();

            worldBlock.set(FastMath.floor(blockLocation.getX()), FastMath.floor(blockLocation.getY()),
                    FastMath.floor(blockLocation.getZ()));
            int blockChunkX = worldBlock.getChunkX();
            int blockChunkY = worldBlock.getChunkY();
            int blockChunkZ = worldBlock.getChunkZ();

            return Math.abs(playerChunkX - blockChunkX) <= client.getChunkDistanceX()
                    && Math.abs(playerChunkY - blockChunkY) <= client.getChunkDistanceY()
                    && Math.abs(playerChunkZ - blockChunkZ) <= client.getChunkDistanceZ();
        } else if (entity.hasComponent(ChunkComponent.class)) {
            return isChunkInViewDistance(
                    entity.getComponent(ChunkComponent.class),
                    clientEntity.getComponent(LocationComponent.class),
                    clientEntity.getComponent(ClientComponent.class));
        }
        return false;
    }

    private boolean isChunkInViewDistance(ChunkComponent chunk, LocationComponent clientLocation, ClientComponent client) {
        // If client is in different world
        if (!clientLocation.getWorldId().equals(chunk.getWorldId()))
            return false;

        worldBlock.set(FastMath.floor(clientLocation.getX()), FastMath.floor(clientLocation.getY()), FastMath.floor(clientLocation.getZ()));
        int playerChunkX = worldBlock.getChunkX();
        int playerChunkY = worldBlock.getChunkY();
        int playerChunkZ = worldBlock.getChunkZ();

        return Math.abs(playerChunkX - chunk.getX()) <= client.getChunkDistanceX()
                && Math.abs(playerChunkY - chunk.getY()) <= client.getChunkDistanceY()
                && Math.abs(playerChunkZ - chunk.getZ()) <= client.getChunkDistanceZ();
    }

    @ReceiveEvent
    public void clientConnected(ClientConnectedEvent event, EntityRef clientEntity, ClientComponent clientComponent, LocationComponent location) {
        List<EntityRef> chunkStateToUpdate = new LinkedList<>();

        for (EntityRef chunkEntity : entityManager.getEntitiesWithComponents(ChunkComponent.class)) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            if (isChunkInViewDistance(chunk, location, clientComponent)) {
                chunkStateToUpdate.add(chunkEntity);
            }
        }

        if (!chunkStateToUpdate.isEmpty()) {
            for (ClientEntityRelevancyRuleListener listener : listeners) {
                listener.entityRelevancyChanged(clientComponent.getClientId(), chunkStateToUpdate);
            }
        }
    }

    @ReceiveEvent
    public void characterMoved(AfterComponentUpdated event, EntityRef entity, LocationComponent locationComponent, ClientComponent client) {
        LocationComponent oldLocation = event.getOldComponent(LocationComponent.class);
        LocationComponent newLocation = event.getNewComponent(LocationComponent.class);

        if (!oldLocation.getWorldId().equals(newLocation.getWorldId())) {
            processMovedBetweenWorlds(client, oldLocation, newLocation);
        } else {
            processMovedWithinWorld(client, oldLocation, newLocation);
        }
    }

    @ReceiveEvent
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef chunkEntity, ChunkComponent chunkComponent) {
        for (EntityRef clientEntity : entityManager.getEntitiesWithComponents(ClientComponent.class, LocationComponent.class)) {
            ClientComponent client = clientEntity.getComponent(ClientComponent.class);
            LocationComponent location = clientEntity.getComponent(LocationComponent.class);
            if (isChunkInViewDistance(chunkComponent, location, client)) {
                for (ClientEntityRelevancyRuleListener listener : listeners) {
                    listener.entityRelevancyChanged(client.getClientId(), Collections.singleton(chunkEntity));
                }
            }
        }
    }

    private void processMovedWithinWorld(ClientComponent client, LocationComponent oldLocation, LocationComponent newLocation) {
        List<EntityRef> chunkStateToUpdate = new LinkedList<>();

        for (EntityRef chunkEntity : entityManager.getEntitiesWithComponents(ChunkComponent.class)) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            if (isChunkInViewDistance(chunk, oldLocation, client) != isChunkInViewDistance(chunk, newLocation, client)) {
                chunkStateToUpdate.add(chunkEntity);
            }
        }
        if (!chunkStateToUpdate.isEmpty()) {
            for (ClientEntityRelevancyRuleListener listener : listeners) {
                listener.entityRelevancyChanged(client.getClientId(), chunkStateToUpdate);
            }
        }
    }

    private void processMovedBetweenWorlds(ClientComponent client, LocationComponent oldLocation, LocationComponent newLocation) {
        List<EntityRef> chunkStateToUpdate = new LinkedList<>();

        Iterable<EntityRef> chunkEntities = entityManager.getEntitiesWithComponents(ChunkComponent.class);
        for (EntityRef chunkEntity : chunkEntities) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);

            if (chunk.getWorldId().equals(oldLocation.getWorldId())
                    && isChunkInViewDistance(chunk, oldLocation, client)) {
                // Chunk was visible before
                chunkStateToUpdate.add(chunkEntity);
            } else if (chunk.getWorldId().equals(newLocation.getWorldId())
                    && isChunkInViewDistance(chunk, newLocation, client)) {
                // Chunk is visible now
                chunkStateToUpdate.add(chunkEntity);
            }
        }
        if (!chunkStateToUpdate.isEmpty()) {
            for (ClientEntityRelevancyRuleListener listener : listeners) {
                listener.entityRelevancyChanged(client.getClientId(), chunkStateToUpdate);
            }
        }
    }
}
