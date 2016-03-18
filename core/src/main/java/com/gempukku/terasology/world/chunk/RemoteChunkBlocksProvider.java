package com.gempukku.terasology.world.chunk;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.terasology.communication.RemoveOldChunk;
import com.gempukku.terasology.communication.StoreNewChunk;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.WorldComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = ChunkBlocksProvider.class)
public class RemoteChunkBlocksProvider implements ChunkBlocksProvider {
    @In
    public EntityManager entityManager;

    private Map<String, Map<Vector3, ChunkBlocks>> chunkBlocks = new HashMap<>();

    @ReceiveEvent
    public void loadChunk(StoreNewChunk chunk, EntityRef clientEntity, ClientComponent client) {
        String worldId = chunk.worldId;
        ChunkBlocks chunkDataHolder = new ChunkBlocks(ChunkBlocks.Status.READY, worldId, chunk.x, chunk.y, chunk.z);
        chunkDataHolder.setBlocks(chunk.blockIds);
        Map<Vector3, ChunkBlocks> chunksInWorld = chunkBlocks.get(worldId);
        if (chunksInWorld == null) {
            chunksInWorld = Collections.synchronizedMap(new HashMap<>());
            chunkBlocks.put(worldId, chunksInWorld);
        }
        chunksInWorld.put(new Vector3(chunk.x, chunk.y, chunk.z), chunkDataHolder);

        getWorldEntity(worldId).send(new AfterChunkLoadedEvent(chunk.x, chunk.y, chunk.z));
    }

    @ReceiveEvent
    public void unloadChunk(RemoveOldChunk chunk, EntityRef clientEntity, ClientComponent client) {
        getWorldEntity(chunk.worldId).send(new BeforeChunkUnloadedEvent(chunk.x, chunk.y, chunk.z));
        chunkBlocks.remove(chunk.worldId, new Vector3(chunk.x, chunk.y, chunk.z));
    }

    private EntityRef getWorldEntity(String worldId) {
        for (EntityRef worldEntity : entityManager.getEntitiesWithComponents(WorldComponent.class)) {
            if (worldId.equals(worldEntity.getComponent(WorldComponent.class).getWorldId())) {
                return worldEntity;
            }
        }
        return null;
    }

    @Override
    public synchronized boolean isChunkLoaded(String worldId, int x, int y, int z) {
        ChunkBlocks chunkBlocks = getChunkBlocks(worldId, x, y, z);
        return chunkBlocks != null && chunkBlocks.getStatus() == ChunkBlocks.Status.READY;
    }

    @Override
    public synchronized ChunkBlocks getChunkBlocks(String worldId, int x, int y, int z) {
        Map<Vector3, ChunkBlocks> chunksInWorld = chunkBlocks.get(worldId);
        if (chunksInWorld == null)
            return null;
        return chunksInWorld.get(new Vector3(x, y, z));
    }

    @Override
    public synchronized short getCommonBlockAt(String worldId, int x, int y, int z) {
        WorldBlock tempWorldBlock = new WorldBlock();
        tempWorldBlock.set(x, y, z);

        ChunkBlocks chunkBlocks = getChunkBlocks(worldId, tempWorldBlock.getChunkX(), tempWorldBlock.getChunkY(), tempWorldBlock.getChunkZ());
        if (chunkBlocks == null || chunkBlocks.getStatus() != ChunkBlocks.Status.READY)
            return -1;

        return chunkBlocks.getCommonBlockAt(tempWorldBlock.getInChunkX(), tempWorldBlock.getInChunkY(), tempWorldBlock.getInChunkZ());
    }

}
