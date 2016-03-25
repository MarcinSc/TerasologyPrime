package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
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
import com.gempukku.terasology.graphics.environment.event.AfterChunkMeshCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkMeshRemoved;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;
import com.gempukku.terasology.world.component.WorldComponent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@RegisterSystem(
        profiles = "generateChunkMeshes",
        shared = ChunkMeshManager.class)
public class OffThreadChunkMeshManager implements ChunkMeshManager, LifeCycleSystem, GameLoopListener {
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
    private ChunkMeshGenerationOrder chunkMeshGenerationOrder;
    @In
    private ChunkMeshGenerator chunkMeshGenerator;

    private final int offlineThreadCount = 3;

    private Multimap<String, ChunkMesh> chunkMeshesInWorld = HashMultimap.create();
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
    public ChunkMesh getChunkMesh(String worldId, int x, int y, int z) {
        for (ChunkMesh chunkMesh : chunkMeshesInWorld.get(worldId)) {
            if (chunkMesh.x == x && chunkMesh.y == y && chunkMesh.z == z) {
                if (chunkMesh.getStatus() == ChunkMesh.Status.DISPOSED)
                    return null;

                return chunkMesh;
            }
        }
        return null;
    }

    @Override
    public void update() {
        for (ChunkMesh chunkMesh : chunkMeshesInWorld.values()) {
            ChunkMesh.Status status = chunkMesh.getStatus();
            if (status == ChunkMesh.Status.NOT_READY) {
                if (chunkMeshGenerator.canPrepareChunkData(
                        chunkMesh.worldId, chunkMesh.x, chunkMesh.y, chunkMesh.z)) {
                    chunkMesh.setStatus(ChunkMesh.Status.QUEUED_FOR_GENERATOR);
                }
            }
            if (status == ChunkMesh.Status.GENERATED) {
                Array<MeshPart> meshParts = chunkMeshGenerator.generateMeshParts(chunkMesh.getGeneratorPreparedObject());
                chunkMesh.setMeshParts(meshParts);
                chunkMesh.setStatus(ChunkMesh.Status.READY);
                EntityRef worldEntity = findWorldEntity(chunkMesh.worldId);
                Gdx.app.debug(OffThreadChunkMeshManager.class.getSimpleName(), "Chunk mesh created: " + chunkMesh.x + "," + chunkMesh.y + "," + chunkMesh.z);
                worldEntity.send(new AfterChunkMeshCreated(
                        chunkMesh.worldId, chunkMesh.x, chunkMesh.y, chunkMesh.z));
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
            ChunkMesh chunkMesh = new ChunkMesh(worldId, x, y, z);
            chunkMeshesInWorld.put(worldId, chunkMesh);
        }
    }

    @ReceiveEvent
    public void chunkUnloaded(BeforeChunkUnloadedEvent event, EntityRef worldEntity, WorldComponent worldComponent) {
        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        synchronized (chunkMeshesInWorld) {
            ChunkMesh chunkMesh = getChunkMesh(worldId, x, y, z);
            if (chunkMesh.getMeshParts() != null) {
                Gdx.app.debug(OffThreadChunkMeshManager.class.getSimpleName(), "Chunk mesh disposed: " + chunkMesh.x + "," + chunkMesh.y + "," + chunkMesh.z);
                worldEntity.send(new BeforeChunkMeshRemoved(worldId, x, y, z));
            }
            chunkMeshesInWorld.remove(chunkMesh.getWorldId(), chunkMesh);
            synchronized (chunkMesh) {
                chunkMesh.setStatus(ChunkMesh.Status.DISPOSED);
                chunkMesh.dispose();
            }
        }
    }

    private class OfflineProcessingThread implements Runnable {
        public void run() {
            while (true) {
                ChunkMesh chunkToProcess = getChunkToProcess();
                if (chunkToProcess != null) {
                    boolean canProcess = false;
                    synchronized (chunkToProcess) {
                        if (chunkToProcess.getStatus() == ChunkMesh.Status.QUEUED_FOR_GENERATOR) {
                            chunkToProcess.setStatus(ChunkMesh.Status.GENERATING);
                            canProcess = true;
                        }
                    }
                    if (canProcess) {
                        Object result = chunkMeshGenerator.prepareChunkDataOffThread(textureAtlasProvider.getTextures(),
                                chunkToProcess.worldId, chunkToProcess.x, chunkToProcess.y, chunkToProcess.z);
                        synchronized (chunkToProcess) {
                            if (chunkToProcess.getStatus() == ChunkMesh.Status.GENERATING) {
                                chunkToProcess.setGeneratorPreparedObject(result);
                                chunkToProcess.setStatus(ChunkMesh.Status.GENERATED);
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

        private ChunkMesh getChunkToProcess() {
            synchronized (chunkMeshesInWorld) {
                return chunkMeshGenerationOrder.getChunkMeshToProcess(chunkMeshesInWorld.values());
            }
        }
    }
}
