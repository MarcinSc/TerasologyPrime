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
import com.gempukku.secsy.network.server.ClientConnectedEvent;
import com.gempukku.secsy.network.server.ClientEntityRelevanceRule;
import com.gempukku.secsy.network.server.ClientEntityRelevancyRuleListener;
import com.gempukku.secsy.network.server.ClientManager;
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkComponent;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.component.BlockComponent;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.MultiverseComponent;
import com.gempukku.terasology.world.component.WorldComponent;

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
    @In
    private ChunkBlocksProvider chunkBlocksProvider;

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
            ChunkComponent chunk = entity.getComponent(ChunkComponent.class);
            return isChunkInViewDistance(
                    chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                    clientEntity.getComponent(LocationComponent.class),
                    clientEntity.getComponent(ClientComponent.class));
        } else if (entity.hasComponent(WorldComponent.class)) {
            WorldComponent world = entity.getComponent(WorldComponent.class);
            LocationComponent location = clientEntity.getComponent(LocationComponent.class);
            return location.getWorldId().equals(world.getWorldId());
        } else if (entity.hasComponent(MultiverseComponent.class)) {
            return true;
        }
        return false;
    }

    private boolean isChunkInViewDistance(String worldId, int chunkX, int chunkY, int chunkZ,
                                          LocationComponent clientLocation, ClientComponent client) {
        // If client is in different world
        if (!clientLocation.getWorldId().equals(worldId))
            return false;

        worldBlock.set(FastMath.floor(clientLocation.getX()), FastMath.floor(clientLocation.getY()), FastMath.floor(clientLocation.getZ()));
        int playerChunkX = worldBlock.getChunkX();
        int playerChunkY = worldBlock.getChunkY();
        int playerChunkZ = worldBlock.getChunkZ();

        return Math.abs(playerChunkX - chunkX) <= client.getChunkDistanceX()
                && Math.abs(playerChunkY - chunkY) <= client.getChunkDistanceY()
                && Math.abs(playerChunkZ - chunkZ) <= client.getChunkDistanceZ();
    }

    @ReceiveEvent
    public void clientConnected(ClientConnectedEvent event, EntityRef clientEntity, ClientComponent clientComponent, LocationComponent location) {
        List<EntityRef> entitiesToUpdate = new LinkedList<>();

        List<StoreNewChunk> storeNewChunks = new LinkedList<>();

        for (EntityRef multiverseEntity : entityManager.getEntitiesWithComponents(MultiverseComponent.class)) {
            entitiesToUpdate.add(multiverseEntity);
        }

        for (EntityRef worldEntity : entityManager.getEntitiesWithComponents(WorldComponent.class)) {
            if (location.getWorldId().equals(worldEntity.getComponent(WorldComponent.class).getWorldId()))
                entitiesToUpdate.add(worldEntity);
        }

        for (EntityRef chunkEntity : entityManager.getEntitiesWithComponents(ChunkComponent.class)) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            if (isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), location, clientComponent)) {
                entitiesToUpdate.add(chunkEntity);

                short[] blocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()).getBlocks();
                storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
            }
        }

        if (!entitiesToUpdate.isEmpty()) {
            for (ClientEntityRelevancyRuleListener listener : listeners) {
                listener.entityRelevancyChanged(clientComponent.getClientId(), entitiesToUpdate);
            }
        }

        for (StoreNewChunk storeNewChunk : storeNewChunks) {
            clientEntity.send(storeNewChunk);
        }
    }

    @ReceiveEvent
    public void characterMoved(AfterComponentUpdated event, EntityRef entity, LocationComponent locationComponent, ClientComponent client) {
        LocationComponent oldLocation = event.getOldComponent(LocationComponent.class);
        LocationComponent newLocation = event.getNewComponent(LocationComponent.class);

        if (!oldLocation.getWorldId().equals(newLocation.getWorldId())) {
            processMovedBetweenWorlds(entity, client, oldLocation, newLocation);
        } else {
            processMovedWithinWorld(entity, client, oldLocation, newLocation);
        }
    }

    @ReceiveEvent
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef worldEntity) {
        String worldId = worldEntity.getComponent(WorldComponent.class).getWorldId();
        EntityRef chunkEntity = null;
        for (EntityRef chunk : entityManager.getEntitiesWithComponents(ChunkComponent.class)) {
            ChunkComponent chunkComp = chunk.getComponent(ChunkComponent.class);
            if (chunkComp.getWorldId().equals(worldId)
                    && chunkComp.getX() == event.x
                    && chunkComp.getY() == event.y
                    && chunkComp.getZ() == event.z) {
                chunkEntity = chunk;
                break;
            }
        }

        for (EntityRef clientEntity : entityManager.getEntitiesWithComponents(ClientComponent.class, LocationComponent.class)) {
            ClientComponent client = clientEntity.getComponent(ClientComponent.class);
            LocationComponent location = clientEntity.getComponent(LocationComponent.class);
            if (isChunkInViewDistance(worldId, event.x, event.y, event.z, location, client)) {
                for (ClientEntityRelevancyRuleListener listener : listeners) {
                    listener.entityRelevancyChanged(client.getClientId(), Collections.singleton(chunkEntity));
                }
                short[] blocks = chunkBlocksProvider.getChunkBlocks(worldId, event.x, event.y, event.z).getBlocks();
                clientEntity.send(new StoreNewChunk(worldId, event.x, event.y, event.z, blocks));
            }
        }
    }

    private void processMovedWithinWorld(EntityRef clientEntity, ClientComponent client, LocationComponent oldLocation, LocationComponent newLocation) {
        List<EntityRef> entitiesToUpdate = new LinkedList<>();

        List<StoreNewChunk> storeNewChunks = new LinkedList<>();
        List<RemoveOldChunk> removeOldChunks = new LinkedList<>();

        for (EntityRef chunkEntity : entityManager.getEntitiesWithComponents(ChunkComponent.class)) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            boolean chunkInOldView = isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), oldLocation, client);
            boolean chunkInNewView = isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), newLocation, client);
            if (chunkInOldView != chunkInNewView) {
                entitiesToUpdate.add(chunkEntity);
                if (chunkInOldView) {
                    removeOldChunks.add(new RemoveOldChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()));
                } else {
                    short[] blocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()).getBlocks();
                    storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
                }
            }
        }

        if (!entitiesToUpdate.isEmpty()) {
            for (ClientEntityRelevancyRuleListener listener : listeners) {
                listener.entityRelevancyChanged(client.getClientId(), entitiesToUpdate);
            }
        }

        for (RemoveOldChunk removeOldChunk : removeOldChunks) {
            clientEntity.send(removeOldChunk);
        }
        for (StoreNewChunk storeNewChunk : storeNewChunks) {
            clientEntity.send(storeNewChunk);
        }
    }

    private void processMovedBetweenWorlds(EntityRef clientEntity, ClientComponent client, LocationComponent oldLocation, LocationComponent newLocation) {
        List<EntityRef> entitiesToUpdate = new LinkedList<>();

        List<StoreNewChunk> storeNewChunks = new LinkedList<>();
        List<RemoveOldChunk> removeOldChunks = new LinkedList<>();

        Iterable<EntityRef> chunkEntities = entityManager.getEntitiesWithComponents(ChunkComponent.class);
        for (EntityRef chunkEntity : chunkEntities) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);

            if (chunk.getWorldId().equals(oldLocation.getWorldId())
                    && isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), oldLocation, client)) {
                // Chunk was visible before
                entitiesToUpdate.add(chunkEntity);
                removeOldChunks.add(new RemoveOldChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()));
            } else if (chunk.getWorldId().equals(newLocation.getWorldId())
                    && isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), newLocation, client)) {
                // Chunk is visible now
                entitiesToUpdate.add(chunkEntity);
                short[] blocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()).getBlocks();
                storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
            }
        }
        for (EntityRef worldEntity : entityManager.getEntitiesWithComponents(WorldComponent.class)) {
            String worldId = worldEntity.getComponent(WorldComponent.class).getWorldId();
            if (worldId.equals(oldLocation.getWorldId()) || worldId.equals(newLocation.getWorldId()))
                entitiesToUpdate.add(worldEntity);
        }

        if (!entitiesToUpdate.isEmpty()) {
            for (ClientEntityRelevancyRuleListener listener : listeners) {
                listener.entityRelevancyChanged(client.getClientId(), entitiesToUpdate);
            }
        }

        for (RemoveOldChunk removeOldChunk : removeOldChunks) {
            clientEntity.send(removeOldChunk);
        }
        for (StoreNewChunk storeNewChunk : storeNewChunks) {
            clientEntity.send(storeNewChunk);
        }
    }
}
