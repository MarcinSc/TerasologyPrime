package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.Gdx;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.environment.event.AfterChunkGeometryCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkGeometryRemoved;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometry;
import com.gempukku.terasology.world.component.WorldComponent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@RegisterSystem(
        profiles = "generateChunkMeshes",
        shared = ChunkGeometryManager.class)
public class OffThreadChunkGeometryManager implements ChunkGeometryManager, LifeCycleSystem, GameLoopListener {
    @In
    private ChunkBlocksProvider chunkBlocksProvider;
    @In
    private CommonBlockManager commonBlockManager;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private TerasologyComponentManager terasologyComponentManager;
    @In
    private ShapeProvider shapeProvider;
    @In
    private GameLoop gameLoop;
    @In
    private EntityManager entityManager;
    @In
    private ChunkGeometryGenerationOrder chunkGeometryGenerationOrder;
    @In
    private ChunkGeometryGenerator chunkGeometryGenerator;

    private final int offlineThreadCount = 3;

    private Multimap<String, ChunkGeometryContainer> chunkMeshesInWorld = HashMultimap.create();
    private OfflineProcessingThread[] offlineProcessingThread;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);

        offlineProcessingThread = new OfflineProcessingThread[offlineThreadCount];
        for (int i = 0; i < offlineThreadCount; i++) {
            offlineProcessingThread[i] = new OfflineProcessingThread();
            Thread thr = new Thread(offlineProcessingThread[i]);
            thr.setName("Chunk-mesh-generation-" + i);
            thr.start();
        }
    }

    @Override
    public ChunkGeometryContainer getChunkGeometry(String worldId, int x, int y, int z) {
        for (ChunkGeometryContainer chunkGeometryContainer : chunkMeshesInWorld.get(worldId)) {
            if (chunkGeometryContainer.x == x && chunkGeometryContainer.y == y && chunkGeometryContainer.z == z) {
                return chunkGeometryContainer;
            }
        }
        return null;
    }

    @Override
    public void update() {
        for (ChunkGeometryContainer chunkGeometryContainer : chunkMeshesInWorld.values()) {
            ChunkGeometryContainer.Status status = chunkGeometryContainer.getStatus();
            if (status == ChunkGeometryContainer.Status.NOT_READY) {
                if (chunkGeometryGenerator.canPrepareChunkData(
                        chunkGeometryContainer.worldId, chunkGeometryContainer.x, chunkGeometryContainer.y, chunkGeometryContainer.z)) {
                    chunkGeometryContainer.setStatus(ChunkGeometryContainer.Status.QUEUED_FOR_GENERATOR);
                }
            }
            if (status == ChunkGeometryContainer.Status.GENERATED) {
                chunkGeometryContainer.setStatus(ChunkGeometryContainer.Status.READY);
                EntityRef worldEntity = findWorldEntity(chunkGeometryContainer.worldId);
                Gdx.app.debug(OffThreadChunkGeometryManager.class.getSimpleName(), "Chunk mesh created: " + chunkGeometryContainer.x + "," + chunkGeometryContainer.y + "," + chunkGeometryContainer.z);
                worldEntity.send(new AfterChunkGeometryCreated(
                        chunkGeometryContainer.worldId, chunkGeometryContainer.x, chunkGeometryContainer.y, chunkGeometryContainer.z));
            }
        }
    }

    private EntityRef findWorldEntity(String worldId) {
        for (EntityRef worldEntity : entityManager.getEntitiesWithComponents(WorldComponent.class)) {
            if (worldId.equals(worldEntity.getComponent(WorldComponent.class).getWorldId()))
                return worldEntity;
        }
        return null;
    }

    @ReceiveEvent
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef worldEntity, WorldComponent worldComponent) {
        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        synchronized (chunkMeshesInWorld) {
            ChunkGeometryContainer chunkGeometryContainer = new ChunkGeometryContainer(worldId, x, y, z);
            chunkMeshesInWorld.put(worldId, chunkGeometryContainer);
        }
    }

    @ReceiveEvent
    public void chunkUnloaded(BeforeChunkUnloadedEvent event, EntityRef worldEntity, WorldComponent worldComponent) {
        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        synchronized (chunkMeshesInWorld) {
            ChunkGeometryContainer chunkGeometryContainer = getChunkGeometry(worldId, x, y, z);
            if (chunkGeometryContainer.getChunkGeometry() != null) {
                Gdx.app.debug(OffThreadChunkGeometryManager.class.getSimpleName(), "Chunk mesh disposed: " + chunkGeometryContainer.x + "," + chunkGeometryContainer.y + "," + chunkGeometryContainer.z);
                worldEntity.send(new BeforeChunkGeometryRemoved(worldId, x, y, z));
            }
            chunkMeshesInWorld.remove(chunkGeometryContainer.getWorldId(), chunkGeometryContainer);
            synchronized (chunkGeometryContainer) {
                chunkGeometryContainer.setStatus(ChunkGeometryContainer.Status.DISPOSED);
            }
        }
    }

    private class OfflineProcessingThread implements Runnable {
        public void run() {
            while (true) {
                ChunkGeometryContainer chunkToProcess = getChunkToProcess();
                if (chunkToProcess != null) {
                    boolean canProcess = false;
                    synchronized (chunkToProcess) {
                        if (chunkToProcess.getStatus() == ChunkGeometryContainer.Status.QUEUED_FOR_GENERATOR) {
                            chunkToProcess.setStatus(ChunkGeometryContainer.Status.GENERATING);
                            canProcess = true;
                        }
                    }
                    if (canProcess) {
                        ChunkGeometry result = chunkGeometryGenerator.prepareChunkGeometryOffThread(textureAtlasProvider.getTextures(),
                                chunkToProcess.worldId, chunkToProcess.x, chunkToProcess.y, chunkToProcess.z);
                        synchronized (chunkToProcess) {
                            if (chunkToProcess.getStatus() == ChunkGeometryContainer.Status.GENERATING) {
                                chunkToProcess.setChunkGeometry(result);
                                chunkToProcess.setStatus(ChunkGeometryContainer.Status.GENERATED);
                            }
                        }
                    }
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException exp) {
                        // Ignore
                    }
                }
            }
        }

        private ChunkGeometryContainer getChunkToProcess() {
            synchronized (chunkMeshesInWorld) {
                return chunkGeometryGenerationOrder.getChunkMeshToProcess(chunkMeshesInWorld.values());
            }
        }
    }
}
