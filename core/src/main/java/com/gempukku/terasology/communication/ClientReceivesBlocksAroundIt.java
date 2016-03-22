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
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkComponent;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.component.BlockComponent;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.LocationComponent;
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

            return Math.abs(playerChunkX - worldBlock.getChunkX()) <= client.getChunkDistanceX()
                    && Math.abs(playerChunkY - worldBlock.getChunkY()) <= client.getChunkDistanceY()
                    && Math.abs(playerChunkZ - worldBlock.getChunkZ()) <= client.getChunkDistanceZ();
        } else if (entity.hasComponent(ChunkComponent.class)) {
            LocationComponent clientLocation = clientEntity.getComponent(LocationComponent.class);
            worldBlock.set(FastMath.floor(clientLocation.getX()), FastMath.floor(clientLocation.getY()), FastMath.floor(clientLocation.getZ()));

            ChunkComponent chunk = entity.getComponent(ChunkComponent.class);
            return isChunkInViewDistance(
                    chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                    clientLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(),
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
                                          String playerWorldId, int playerChunkX, int playerChunkY, int playerChunkZ,
                                          ClientComponent client) {
        // If client is in different world
        if (!playerWorldId.equals(worldId))
            return false;

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

        worldBlock.set(FastMath.floor(location.getX()), FastMath.floor(location.getY()), FastMath.floor(location.getZ()));
        int playerChunkX = worldBlock.getChunkX();
        int playerChunkY = worldBlock.getChunkY();
        int playerChunkZ = worldBlock.getChunkZ();
        for (EntityRef chunkEntity : entityManager.getEntitiesWithComponents(ChunkComponent.class)) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            if (isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                    location.getWorldId(), playerChunkX, playerChunkY, playerChunkZ, clientComponent)) {
                entitiesToUpdate.add(chunkEntity);

                short[] blocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()).getBlocks();
                storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
            }
        }

        for (EntityRef blockEntity : entityManager.getEntitiesWithComponents(BlockComponent.class)) {
            LocationComponent blockLocation = blockEntity.getComponent(LocationComponent.class);

            worldBlock.set(FastMath.floor(blockLocation.getX()), FastMath.floor(blockLocation.getY()),
                    FastMath.floor(blockLocation.getZ()));

            if (Math.abs(playerChunkX - worldBlock.getChunkX()) <= clientComponent.getChunkDistanceX()
                    && Math.abs(playerChunkY - worldBlock.getChunkY()) <= clientComponent.getChunkDistanceY()
                    && Math.abs(playerChunkZ - worldBlock.getChunkZ()) <= clientComponent.getChunkDistanceZ())
                entitiesToUpdate.add(blockEntity);
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

        if (oldLocation != null && newLocation != null) {
            if (!oldLocation.getWorldId().equals(newLocation.getWorldId())) {
                processMovedBetweenWorlds(entity, client, oldLocation, newLocation);
            } else {
                processMovedWithinWorld(entity, client, oldLocation, newLocation);
            }
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
            LocationComponent clientLocation = clientEntity.getComponent(LocationComponent.class);
            worldBlock.set(FastMath.floor(clientLocation.getX()), FastMath.floor(clientLocation.getY()), FastMath.floor(clientLocation.getZ()));

            ClientComponent client = clientEntity.getComponent(ClientComponent.class);
            if (isChunkInViewDistance(worldId, event.x, event.y, event.z,
                    clientLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(), client)) {
                for (ClientEntityRelevancyRuleListener listener : listeners) {
                    listener.entityRelevancyChanged(client.getClientId(), Collections.singleton(chunkEntity));
                }
                ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, event.x, event.y, event.z);
                if (chunkBlocks == null) {
                    chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, event.x, event.y, event.z);
                    System.out.println("Processing chunk loaded: " + event.x + "," + event.y + "," + event.z);
                }
                short[] blocks = chunkBlocks.getBlocks();
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

            worldBlock.set(FastMath.floor(oldLocation.getX()), FastMath.floor(oldLocation.getY()), FastMath.floor(oldLocation.getZ()));
            boolean chunkInOldView = isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                    oldLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(), client);

            worldBlock.set(FastMath.floor(newLocation.getX()), FastMath.floor(newLocation.getY()), FastMath.floor(newLocation.getZ()));
            boolean chunkInNewView = isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                    newLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(), client);
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

            if (chunk.getWorldId().equals(oldLocation.getWorldId())) {
                worldBlock.set(FastMath.floor(oldLocation.getX()), FastMath.floor(oldLocation.getY()), FastMath.floor(oldLocation.getZ()));
                if (isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                        oldLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(), client)) {
                    // Chunk was visible before
                    entitiesToUpdate.add(chunkEntity);
                    removeOldChunks.add(new RemoveOldChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()));
                }
            } else if (chunk.getWorldId().equals(newLocation.getWorldId())) {
                worldBlock.set(FastMath.floor(newLocation.getX()), FastMath.floor(newLocation.getY()), FastMath.floor(newLocation.getZ()));
                if (isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                        newLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(), client)) {
                    // Chunk is visible now
                    entitiesToUpdate.add(chunkEntity);
                    short[] blocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()).getBlocks();
                    storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
                }
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
