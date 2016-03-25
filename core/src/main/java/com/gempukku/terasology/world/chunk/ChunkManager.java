package com.gempukku.terasology.world.chunk;

import com.badlogic.gdx.Gdx;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.secsy.entity.io.StoredEntityData;
import com.gempukku.secsy.entity.relevance.EntityRelevanceRule;
import com.gempukku.secsy.entity.relevance.EntityRelevanceRuleRegistry;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.MultiverseManager;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;
import com.gempukku.terasology.world.component.BlockComponent;
import com.gempukku.terasology.world.component.LocationComponent;
import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = {ChunkBlocksProvider.class, ChunkRelevanceRuleRegistry.class})
public class ChunkManager implements EntityRelevanceRule, ChunkBlocksProvider, ChunkRelevanceRuleRegistry,
        LifeCycleSystem {
    @In
    private EntityRelevanceRuleRegistry registry;
    @In
    private EntityManager entityManager;
    @In
    private EntityIndexManager entityIndexManager;
    @In
    private MultiverseManager multiverseManager;
    @In
    private WorldStorage worldStorage;
    @In
    private WorldGenerator worldGenerator;
    @In
    private CommonBlockManager commonBlockManager;

    private List<ChunkRelevanceRule> chunkRelevanceRules = new LinkedList<>();

    private final int offlineThreadCount = 4;
    private OfflineProcessingThread[] offlineProcessingThread;

    // This is being accessed both by main thread, as well as generating threads
    private Map<IntLocationKey, ChunkBlocks> chunkBlocks = Collections.synchronizedMap(new HashMap<>());

    private List<Iterable<StoredEntityData>> entitiesToAdd = new LinkedList<>();
    private List<EntityRef> entitiesToRemove = new LinkedList<>();

    private List<ChunkBlocks> chunksToNotify = new LinkedList<>();

    private final Object copyLockObject = new Object();

    private Map<ChunkBlocks, Iterable<StoredEntityData>> finishedBlocksOffMainThread = new HashMap<>();
    private EntityIndex blockIndex;
    private EntityIndex chunkIndex;

    @Override
    public void initialize() {
        registry.registerEntityRelevanceRule(this);

        offlineProcessingThread = new OfflineProcessingThread[offlineThreadCount];
        for (int i = 0; i < offlineThreadCount; i++) {
            offlineProcessingThread[i] = new OfflineProcessingThread();
            Thread thr = new Thread(offlineProcessingThread[i]);
            thr.setName("Chunk-generation-" + i);
            thr.start();
        }

        blockIndex = entityIndexManager.addIndexOnComponents(BlockComponent.class);
        chunkIndex = entityIndexManager.addIndexOnComponents(ChunkComponent.class);
    }

    @Override
    public void registerChunkRelevanceRule(ChunkRelevanceRule chunkRelevanceRule) {
        chunkRelevanceRules.add(chunkRelevanceRule);
    }

    private WorldBlock tempBlock = new WorldBlock();

    @Override
    public void determineRelevance() {
        entitiesToAdd.clear();
        entitiesToRemove.clear();
        chunksToNotify.clear();

        // Bring in all the offThread values into main thread
        synchronized (copyLockObject) {
            Gdx.app.debug("ChunkManager", "To merge: " + finishedBlocksOffMainThread.size() + " chunks.");

            Iterator<Map.Entry<ChunkBlocks, Iterable<StoredEntityData>>> iterator = finishedBlocksOffMainThread.entrySet().iterator();
            int count = 0;
            // We merge 50 chunks at a time...
            while (iterator.hasNext() && count < 50) {
                Map.Entry<ChunkBlocks, Iterable<StoredEntityData>> entry = iterator.next();
                ChunkBlocks chunkBlocks = entry.getKey();
                entitiesToAdd.add(entry.getValue());
                chunksToNotify.add(chunkBlocks);
                Gdx.app.debug("ChunkManager", "Merged chunk: " + chunkBlocks.x + "," + chunkBlocks.y + "," + chunkBlocks.z);
                iterator.remove();
                count++;
            }
        }

        // To avoid ConcurrentModificationException we first gather the ones to remove
        Set<ChunkBlocks> toRemove = new HashSet<>();
        for (ChunkBlocks blocks : chunkBlocks.values()) {
            if (!isChunkRelevant(blocks)) {
                Gdx.app.debug("ChunkManager", "Unloading chunk: " + blocks.x + "," + blocks.y + "," + blocks.z);

                if (blocks.getStatus() == ChunkBlocks.Status.READY) {
                    multiverseManager.getWorldEntity(blocks.worldId).send(
                            new BeforeChunkUnloadedEvent(blocks.x, blocks.y, blocks.z));
                }

                synchronized (copyLockObject) {
                    // Make sure we remove any un-merged data about that chunk
                    finishedBlocksOffMainThread.remove(blocks);
                    chunksToNotify.remove(blocks);
                }

                EntityRef chunkEntity = getChunkEntity(blocks);
                if (chunkEntity != null) {
                    entitiesToRemove.add(chunkEntity);
                    for (EntityRef blockEntity : blockIndex.getEntities()) {
                        LocationComponent location = blockEntity.getComponent(LocationComponent.class);
                        if (location.getWorldId().equals(blocks.getWorldId())) {
                            tempBlock.set(location.getX(), location.getY(), location.getZ());
                            if (tempBlock.getChunkX() == blocks.x
                                    && tempBlock.getChunkY() == blocks.y
                                    && tempBlock.getChunkZ() == blocks.z)
                                entitiesToRemove.add(blockEntity);
                        }
                    }
                }

                toRemove.add(blocks);
            }
        }

        synchronized (chunkBlocks) {
            // And now remove them
            for (ChunkBlocks blocks : toRemove) {
                chunkBlocks.remove(new IntLocationKey(blocks));
            }
        }

        for (ChunkRelevanceRule chunkRelevanceRule : chunkRelevanceRules) {
            for (ChunkLocation chunkLocation : chunkRelevanceRule.getRelevantChunks()) {
                ensureChunkLoaded(chunkLocation.getWorldId(), chunkLocation.getX(), chunkLocation.getY(), chunkLocation.getZ());
            }
        }
    }

    private boolean isChunkRelevant(ChunkBlocks chunkBlocks) {
        for (ChunkRelevanceRule chunkRelevanceRule : chunkRelevanceRules) {
            if (chunkRelevanceRule.isChunkRelevant(chunkBlocks))
                return true;
        }
        return false;
    }

    @Override
    public Iterable<? extends StoredEntityData> getNewRelevantEntities() {
        return Iterables.concat(entitiesToAdd);
    }

    @Override
    public Iterable<? extends EntityRef> getNotRelevantEntities() {
        return Iterables.concat(entitiesToRemove);
    }

    @Override
    public void storeEntities(Iterable<? extends StoredEntityData> iterable) {
        // TODO store entities in storage
    }

    @Override
    public void newRelevantEntitiesLoaded() {
        // We have to assign to ChunkBlocks the entity that it represents
        for (ChunkBlocks chunkBlocks : chunksToNotify) {
            EntityRef chunkEntity = getChunkEntity(chunkBlocks);
            chunkBlocks.setBlocks(chunkEntity.getComponent(ChunkComponent.class).getBlockIds());
            chunkBlocks.setStatus(ChunkBlocks.Status.READY);

            Gdx.app.debug("ChunkManager", "Notifying on: " + chunkBlocks.getX() + "," + chunkBlocks.getY() + "," + chunkBlocks.getZ());
            multiverseManager.getWorldEntity(chunkBlocks.getWorldId()).send(
                    new AfterChunkLoadedEvent(chunkBlocks.getX(), chunkBlocks.getY(), chunkBlocks.getZ()));
        }
    }

    @Override
    public short getCommonBlockAt(String worldId, int x, int y, int z) {
        WorldBlock tempWorldBlock = new WorldBlock();
        tempWorldBlock.set(x, y, z);

        ChunkBlocks chunkBlocks = getChunkBlocks(worldId, tempWorldBlock.getChunkX(), tempWorldBlock.getChunkY(), tempWorldBlock.getChunkZ());
        if (chunkBlocks == null || chunkBlocks.getStatus() != ChunkBlocks.Status.READY)
            return -1;

        return chunkBlocks.getCommonBlockAt(tempWorldBlock.getInChunkX(), tempWorldBlock.getInChunkY(), tempWorldBlock.getInChunkZ());
    }

    @Override
    public boolean isChunkLoaded(String worldId, int x, int y, int z) {
        ChunkBlocks chunkBlocks = getChunkBlocks(worldId, x, y, z);
        return chunkBlocks != null && chunkBlocks.getStatus() == ChunkBlocks.Status.READY;
    }

    private ChunkBlocks getChunkBlocksInternal(String worldId, int x, int y, int z) {
        return chunkBlocks.get(new IntLocationKey(worldId, x, y, z));
    }

    @Override
    public ChunkBlocks getChunkBlocks(String worldId, int x, int y, int z) {
        ChunkBlocks chunkBlocks = getChunkBlocksInternal(worldId, x, y, z);
        if (chunkBlocks == null || chunkBlocks.getStatus() != ChunkBlocks.Status.READY)
            return null;
        return chunkBlocks;
    }

    private void ensureChunkLoaded(String worldId, int x, int y, int z) {
        if (getChunkBlocksInternal(worldId, x, y, z) == null) {
            loadOrGenerateChunk(worldId, x, y, z);
        }
    }

    private void loadOrGenerateChunk(String worldId, int x, int y, int z) {
        synchronized (chunkBlocks) {
            ChunkBlocks chunkDataHolder = new ChunkBlocks(ChunkBlocks.Status.QUEUED, worldId, x, y, z);
            chunkBlocks.put(new IntLocationKey(chunkDataHolder), chunkDataHolder);
        }
    }

    private EntityRef getChunkEntity(ChunkLocation chunkLocation) {
        for (EntityRef chunkEntity : chunkIndex.getEntities()) {
            ChunkComponent chunk = chunkEntity.getComponent(ChunkComponent.class);
            if (chunk.getWorldId().equals(chunkLocation.getWorldId())
                    && chunk.getX() == chunkLocation.getX()
                    && chunk.getY() == chunkLocation.getY()
                    && chunk.getZ() == chunkLocation.getZ())
                return chunkEntity;
        }
        return null;
    }

    private class OfflineProcessingThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                ChunkBlocks chunkToProcess = selectChunkBlocksToGenerate();
                if (chunkToProcess != null) {
                    generateChunk(chunkToProcess);
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException exp) {
                        // Ignore
                    }
                }
            }
        }

        private void generateChunk(ChunkBlocks chunkBlocks) {
            Iterable<StoredEntityData> chunkData = worldGenerator.generateChunk(chunkBlocks.worldId, chunkBlocks.x, chunkBlocks.y, chunkBlocks.z);

            synchronized (copyLockObject) {
                finishedBlocksOffMainThread.put(chunkBlocks, chunkData);
            }
        }

        private ChunkBlocks selectChunkBlocksToGenerate() {
            synchronized (chunkBlocks) {
                for (ChunkBlocks blocks : chunkBlocks.values()) {
                    if (blocks.getStatus() == ChunkBlocks.Status.QUEUED) {
                        blocks.setStatus(ChunkBlocks.Status.GENERATING);
                        return blocks;
                    }
                }
            }
            return null;
        }
    }
}
