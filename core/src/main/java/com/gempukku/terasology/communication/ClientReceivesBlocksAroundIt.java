package com.gempukku.terasology.communication;

import com.badlogic.gdx.Gdx;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.context.util.PriorityCollection;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
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

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class ClientReceivesBlocksAroundIt implements ClientEntityRelevanceRule, LifeCycleSystem {
    @In
    private EntityManager entityManager;
    @In
    private EntityIndexManager entityIndexManager;
    @In
    private ClientManager clientManager;
    @In
    private ChunkBlocksProvider chunkBlocksProvider;

    private PriorityCollection<ClientEntityRelevancyRuleListener> listeners = new PriorityCollection<>();

    private EntityIndex multiverseIndex;
    private EntityIndex worldIndex;
    private EntityIndex chunkIndex;
    private EntityIndex blockIndex;
    private EntityIndex clientAndLocationIndex;

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
        multiverseIndex = entityIndexManager.addIndexOnComponents(MultiverseComponent.class);
        worldIndex = entityIndexManager.addIndexOnComponents(WorldComponent.class);
        chunkIndex = entityIndexManager.addIndexOnComponents(ChunkComponent.class);
        blockIndex = entityIndexManager.addIndexOnComponents(BlockComponent.class);
        clientAndLocationIndex = entityIndexManager.addIndexOnComponents(ClientComponent.class, LocationComponent.class);
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

        for (EntityRef multiverseEntity : multiverseIndex.getEntities()) {
            entitiesToUpdate.add(multiverseEntity);
        }

        for (EntityRef worldEntity : worldIndex.getEntities()) {
            if (location.getWorldId().equals(worldEntity.getComponent(WorldComponent.class).getWorldId()))
                entitiesToUpdate.add(worldEntity);
        }

        List<StoreNewChunk> storeNewChunks = new LinkedList<>();

        worldBlock.set(FastMath.floor(location.getX()), FastMath.floor(location.getY()), FastMath.floor(location.getZ()));
        int playerChunkX = worldBlock.getChunkX();
        int playerChunkY = worldBlock.getChunkY();
        int playerChunkZ = worldBlock.getChunkZ();
        for (EntityRef chunkEntity : chunkIndex.getEntities()) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            if (isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                    location.getWorldId(), playerChunkX, playerChunkY, playerChunkZ, clientComponent)) {
                entitiesToUpdate.add(chunkEntity);

                short[] blocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()).getBlocks();
                storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
            }
        }

        for (EntityRef blockEntity : blockIndex.getEntities()) {
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
    public void characterMoved(AfterComponentUpdated event, EntityRef entity, LocationComponent locationComponent) {
        ClientComponent client = entity.getComponent(ClientComponent.class);
        if (client != null) {
            LocationComponent oldLocation = event.getOldComponent(LocationComponent.class);
            LocationComponent newLocation = event.getNewComponent(LocationComponent.class);

            if (oldLocation != null && newLocation != null) {
                if (!oldLocation.getWorldId().equals(newLocation.getWorldId())) {
                    processPlayerMovedBetweenWorlds(entity, client, oldLocation, newLocation);
                } else {
                    processPlayerMovedWithinWorld(entity, client, oldLocation, newLocation);
                }
            }
        }
    }

    @ReceiveEvent
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef worldEntity) {
        String worldId = worldEntity.getComponent(WorldComponent.class).getWorldId();
        int chunkX = event.x;
        int chunkY = event.y;
        int chunkZ = event.z;
        EntityRef chunkEntity = getChunkEntity(worldId, chunkX, chunkY, chunkZ);

        for (EntityRef clientEntity : clientAndLocationIndex.getEntities()) {
            List<EntityRef> changeRelevanceEntities = new LinkedList<>();

            LocationComponent clientLocation = clientEntity.getComponent(LocationComponent.class);
            worldBlock.set(FastMath.floor(clientLocation.getX()), FastMath.floor(clientLocation.getY()), FastMath.floor(clientLocation.getZ()));

            ClientComponent client = clientEntity.getComponent(ClientComponent.class);
            if (isChunkInViewDistance(worldId, chunkX, chunkY, chunkZ,
                    clientLocation.getWorldId(), worldBlock.getChunkX(), worldBlock.getChunkY(), worldBlock.getChunkZ(), client)) {
                changeRelevanceEntities.add(chunkEntity);

                for (EntityRef blockEntity : blockIndex.getEntities()) {
                    LocationComponent blockLocation = blockEntity.getComponent(LocationComponent.class);
                    if (blockLocation.getWorldId().equals(worldId)) {
                        worldBlock.set(FastMath.floor(blockLocation.getX()), FastMath.floor(blockLocation.getY()), FastMath.floor(blockLocation.getZ()));
                        if (worldBlock.getChunkX() == chunkX && worldBlock.getChunkY() == chunkY && worldBlock.getChunkZ() == chunkZ)
                            changeRelevanceEntities.add(blockEntity);
                    }
                }

                short[] blocks = chunkBlocksProvider.getChunkBlocks(worldId, chunkX, chunkY, chunkZ).getBlocks();
                Gdx.app.debug("ClientReceivesBlocksAroundIt", "Sending chunk to client: " + chunkX + "," + chunkY + "," + chunkZ);
                clientEntity.send(new StoreNewChunk(worldId, chunkX, chunkY, chunkZ, blocks));
            }

            if (changeRelevanceEntities.size() > 0) {
                for (ClientEntityRelevancyRuleListener listener : listeners) {
                    listener.entityRelevancyChanged(client.getClientId(), changeRelevanceEntities);
                }
            }
        }
    }

    private EntityRef getChunkEntity(String worldId, int chunkX, int chunkY, int chunkZ) {
        for (EntityRef chunk : chunkIndex.getEntities()) {
            ChunkComponent chunkComp = chunk.getComponent(ChunkComponent.class);
            if (chunkComp.getWorldId().equals(worldId)
                    && chunkComp.getX() == chunkX
                    && chunkComp.getY() == chunkY
                    && chunkComp.getZ() == chunkZ) {
                return chunk;
            }
        }
        return null;
    }

    private void processPlayerMovedWithinWorld(EntityRef clientEntity, ClientComponent client, LocationComponent oldLocation, LocationComponent newLocation) {
        worldBlock.set(FastMath.floor(oldLocation.getX()), FastMath.floor(oldLocation.getY()), FastMath.floor(oldLocation.getZ()));
        int oldChunkX = worldBlock.getChunkX();
        int oldChunkY = worldBlock.getChunkY();
        int oldChunkZ = worldBlock.getChunkZ();

        worldBlock.set(FastMath.floor(newLocation.getX()), FastMath.floor(newLocation.getY()), FastMath.floor(newLocation.getZ()));
        int newChunkX = worldBlock.getChunkX();
        int newChunkY = worldBlock.getChunkY();
        int newChunkZ = worldBlock.getChunkZ();

        // Player moved from chunk to chunk
        if (oldChunkX != newChunkX || oldChunkY != newChunkY || oldChunkZ != newChunkZ) {
            List<EntityRef> entitiesToUpdate = new LinkedList<>();

            List<StoreNewChunk> storeNewChunks = new LinkedList<>();
            List<RemoveOldChunk> removeOldChunks = new LinkedList<>();

            for (EntityRef chunkEntity : chunkIndex.getEntities()) {
                ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);

                boolean chunkInOldView = isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                        oldLocation.getWorldId(), oldChunkX, oldChunkY, oldChunkZ, client);

                boolean chunkInNewView = isChunkInViewDistance(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(),
                        newLocation.getWorldId(), newChunkX, newChunkY, newChunkZ, client);
                if (chunkInOldView != chunkInNewView) {
                    entitiesToUpdate.add(chunkEntity);

                    appendBlocksInChunk(entitiesToUpdate, chunk, oldLocation.getWorldId());
                    if (chunkInOldView) {
                        removeOldChunks.add(new RemoveOldChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ()));
                    } else {
                        ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ());
                        if (chunkBlocks != null) {
                            short[] blocks = chunkBlocks.getBlocks();
                            storeNewChunks.add(new StoreNewChunk(chunk.getWorldId(), chunk.getX(), chunk.getY(), chunk.getZ(), blocks));
                        }
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
    }

    private void appendBlocksInChunk(List<EntityRef> entitiesToUpdate, ChunkComponent chunk, String worldId) {
        for (EntityRef blockEntity : blockIndex.getEntities()) {
            LocationComponent blockLocation = blockEntity.getComponent(LocationComponent.class);
            if (blockLocation.getWorldId().equals(worldId)) {
                worldBlock.set(FastMath.floor(blockLocation.getX()), FastMath.floor(blockLocation.getY()), FastMath.floor(blockLocation.getZ()));
                if (worldBlock.getChunkX() == chunk.getX() && worldBlock.getChunkY() == chunk.getY() && worldBlock.getChunkZ() == chunk.getZ())
                    entitiesToUpdate.add(blockEntity);
            }
        }
    }

    private void processPlayerMovedBetweenWorlds(EntityRef clientEntity, ClientComponent client, LocationComponent oldLocation, LocationComponent newLocation) {
        // TODO implement it correctly
    }
}
