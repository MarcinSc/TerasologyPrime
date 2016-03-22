package com.gempukku.terasology.world.chunk;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.secsy.entity.relevance.EntityRelevanceRule;
import com.gempukku.secsy.entity.relevance.EntityRelevanceRuleRegistry;
import com.gempukku.secsy.network.serialize.ComponentInformation;
import com.gempukku.secsy.network.serialize.EntityInformation;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.MultiverseManager;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.component.BlockComponent;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.LocationComponent;
import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = ChunkBlocksProvider.class)
public class ChunkManager implements EntityRelevanceRule, ChunkBlocksProvider, LifeCycleSystem {
    @In
    private EntityRelevanceRuleRegistry registry;
    @In
    private EntityManager entityManager;
    @In
    private MultiverseManager multiverseManager;
    @In
    private WorldStorage worldStorage;
    @In
    private ChunkGenerator chunkGenerator;
    @In
    private CommonBlockManager commonBlockManager;

    private final int offlineThreadCount = 3;

    private OfflineProcessingThread[] offlineProcessingThread;

    // This is being accessed both by main thread, as well as generating threads
    private Map<String, Map<Vector3, ChunkBlocks>> chunkBlocks = Collections.synchronizedMap(new HashMap<>());

    private List<Iterable<EntityData>> entitiesToConsume = new LinkedList<>();
    private List<ChunkLocation> chunksToNotify = new LinkedList<>();

    private final Object copyLockObject = new Object();

    private List<ChunkBlocks> finishedBlocksOffMainThread = new LinkedList<>();
    private List<Iterable<EntityData>> entitiesToConsumeOffMainThread = new LinkedList<>();

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
    }

    @Override
    public void determineRelevance() {
        entitiesToConsume.clear();
        chunksToNotify.clear();

        // Bring in all the offThread values into main thread
        synchronized (copyLockObject) {
            for (ChunkBlocks finishedBlock : finishedBlocksOffMainThread) {
                finishedBlock.setStatus(ChunkBlocks.Status.READY);
            }
            entitiesToConsume.addAll(entitiesToConsumeOffMainThread);
            chunksToNotify.addAll(finishedBlocksOffMainThread);

            finishedBlocksOffMainThread.clear();
            entitiesToConsumeOffMainThread.clear();
        }

        for (EntityRef player : entityManager.getEntitiesWithComponents(ClientComponent.class, LocationComponent.class)) {
            ClientComponent comp = player.getComponent(ClientComponent.class);
            int chunkDistanceX = comp.getChunkDistanceX();
            int chunkDistanceY = comp.getChunkDistanceY();
            int chunkDistanceZ = comp.getChunkDistanceZ();
            LocationComponent location = player.getComponent(LocationComponent.class);
            Vector3 chunkLocation = getChunkLocation(location.getX(), location.getY(), location.getZ());
            String worldId = location.getWorldId();

            for (int x = -chunkDistanceX; x <= chunkDistanceX; x++) {
                for (int y = -chunkDistanceY; y <= chunkDistanceY; y++) {
                    for (int z = -chunkDistanceZ; z <= chunkDistanceZ; z++) {
                        ensureChunkLoaded(worldId, Math.round(chunkLocation.x + x), Math.round(chunkLocation.y + y), Math.round(chunkLocation.z + z));
                    }
                }
            }
        }
    }

    @Override
    public Iterable<? extends EntityData> getNewRelevantEntities() {
        return Iterables.concat(entitiesToConsume);
    }

    @Override
    public Iterable<? extends EntityRef> getNotRelevantEntities() {
        return Collections.emptySet();
    }

    @Override
    public void storeEntities(Iterable<? extends EntityData> iterable) {
        // TODO store entities in storage
    }

    @Override
    public void newRelevantEntitiesLoaded() {
        // We have to assign to ChunkBlocks the entity that it represents
        for (ChunkLocation chunkLocation : chunksToNotify) {
            multiverseManager.getWorldEntity(chunkLocation.getWorldId()).send(
                    new AfterChunkLoadedEvent(chunkLocation.getX(), chunkLocation.getY(), chunkLocation.getZ()));
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
        Map<Vector3, ChunkBlocks> chunksInWorld = chunkBlocks.get(worldId);
        if (chunksInWorld == null)
            return null;
        return chunksInWorld.get(new Vector3(x, y, z));
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
            Map<Vector3, ChunkBlocks> chunksInWorld = chunkBlocks.get(worldId);
            if (chunksInWorld == null) {
                chunksInWorld = Collections.synchronizedMap(new HashMap<>());
                chunkBlocks.put(worldId, chunksInWorld);
            }
            chunksInWorld.put(new Vector3(x, y, z), chunkDataHolder);
        }
    }

    private Vector3 tempVec = new Vector3();

    private Vector3 getChunkLocation(float x, float y, float z) {
        tempVec.set(
                (float) Math.floor(x / ChunkSize.X),
                (float) Math.floor(y / ChunkSize.Y),
                (float) Math.floor(z / ChunkSize.Z));
        return tempVec;
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
            Iterable<ChunkGenerator.EntityDataOrCommonBlock> chunkData = chunkGenerator.generateChunk(chunkBlocks.worldId, chunkBlocks.x, chunkBlocks.y, chunkBlocks.z);

            short[] chunkBlockIds = new short[ChunkSize.X * ChunkSize.Y * ChunkSize.Z];

            Set<EntityData> entities = new HashSet<>();
            int index = 0;
            for (ChunkGenerator.EntityDataOrCommonBlock blockInfo : chunkData) {
                int blockInChunkX = index / (ChunkSize.Y * ChunkSize.Z);
                int blockInChunkY = (index / ChunkSize.Z) % ChunkSize.Y;
                int blockInChunkZ = index % ChunkSize.Z;

                chunkBlockIds[index] = blockInfo.commonBlock;
                if (blockInfo.entityData != null) {
                    EntityInformation entityData = new EntityInformation(blockInfo.entityData);
                    ComponentInformation locationComponentData = new ComponentInformation(LocationComponent.class);
                    locationComponentData.addField("worldId", chunkBlocks.worldId);
                    locationComponentData.addField("x", (float) (chunkBlocks.x * ChunkSize.X + blockInChunkX));
                    locationComponentData.addField("y", (float) (chunkBlocks.y * ChunkSize.Y + blockInChunkY));
                    locationComponentData.addField("z", (float) (chunkBlocks.z * ChunkSize.Z + blockInChunkZ));

                    entityData.addComponent(locationComponentData);
                    entityData.addComponent(new ComponentInformation(BlockComponent.class));
                    entities.add(entityData);
                }
                index++;
            }

            chunkBlocks.setBlocks(chunkBlockIds);

            EntityInformation chunkEntity = new EntityInformation();
            ComponentInformation chunkComponent = new ComponentInformation(ChunkComponent.class);
            chunkComponent.addField("worldId", chunkBlocks.worldId);
            chunkComponent.addField("x", chunkBlocks.x);
            chunkComponent.addField("y", chunkBlocks.y);
            chunkComponent.addField("z", chunkBlocks.z);
            chunkEntity.addComponent(chunkComponent);
            entities.add(chunkEntity);

            synchronized (copyLockObject) {
                finishedBlocksOffMainThread.add(chunkBlocks);
                entitiesToConsumeOffMainThread.add(entities);
            }
        }

        private ChunkBlocks selectChunkBlocksToGenerate() {
            synchronized (chunkBlocks) {
                for (Map<Vector3, ChunkBlocks> vector3ChunkBlocksMap : chunkBlocks.values()) {
                    for (ChunkBlocks blocks : vector3ChunkBlocksMap.values()) {
                        if (blocks.getStatus() == ChunkBlocks.Status.QUEUED) {
                            blocks.setStatus(ChunkBlocks.Status.GENERATING);
                            return blocks;
                        }
                    }
                }
            }
            return null;
        }
    }
}
