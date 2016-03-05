package com.gempukku.terasology.world.chunk;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = ChunkBlocksProvider.class)
public class RemoteChunkBlocksProvider implements ChunkBlocksProvider {
    private Map<String, Map<Vector3, ChunkBlocks>> chunkBlocks = new HashMap<>();

    @ReceiveEvent
    public void componentAdded(AfterComponentAdded event, EntityRef chunkEntity, ChunkComponent chunk) {
        String worldId = chunk.getWorldId();
        ChunkBlocks chunkDataHolder = new ChunkBlocks(ChunkBlocks.Status.READY, worldId, chunk.getX(), chunk.getY(), chunk.getZ());
        chunkDataHolder.setChunkEntity(chunkEntity);
        Map<Vector3, ChunkBlocks> chunksInWorld = chunkBlocks.get(worldId);
        if (chunksInWorld == null) {
            chunksInWorld = Collections.synchronizedMap(new HashMap<>());
            chunkBlocks.put(worldId, chunksInWorld);
        }
        chunksInWorld.put(new Vector3(chunk.getX(), chunk.getY(), chunk.getZ()), chunkDataHolder);
        chunkEntity.send(AfterChunkLoadedEvent.INSTANCE);
    }

    @ReceiveEvent
    public void componentRemoved(BeforeComponentRemoved event, EntityRef chunkEntity, ChunkComponent chunk) {
        chunkEntity.send(BeforeChunkUnloadedEvent.INSTANCE);
        String worldId = chunk.getWorldId();
        chunkBlocks.get(worldId).remove(new Vector3(chunk.getX(), chunk.getY(), chunk.getZ()));
    }

    @Override
    public boolean isChunkLoaded(String worldId, int x, int y, int z) {
        ChunkBlocks chunkBlocks = getChunkBlocks(worldId, x, y, z);
        return chunkBlocks != null && chunkBlocks.getStatus() == ChunkBlocks.Status.READY;
    }

    @Override
    public ChunkBlocks getChunkBlocks(String worldId, int x, int y, int z) {
        Map<Vector3, ChunkBlocks> chunksInWorld = chunkBlocks.get(worldId);
        if (chunksInWorld == null)
            return null;
        return chunksInWorld.get(new Vector3(x, y, z));
    }

    @Override
    public String getCommonBlockAt(String worldId, int x, int y, int z) {
        WorldBlock tempWorldBlock = new WorldBlock();
        tempWorldBlock.set(x, y, z);

        ChunkBlocks chunkBlocks = getChunkBlocks(worldId, tempWorldBlock.getChunkX(), tempWorldBlock.getChunkY(), tempWorldBlock.getChunkZ());
        if (chunkBlocks == null || chunkBlocks.getStatus() != ChunkBlocks.Status.READY)
            return null;

        return chunkBlocks.getCommonBlockAt(tempWorldBlock.getInChunkX(), tempWorldBlock.getInChunkY(), tempWorldBlock.getInChunkZ());
    }

}
