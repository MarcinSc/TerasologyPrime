package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.environment.EnvironmentRenderer;
import com.gempukku.terasology.graphics.environment.EnvironmentRendererRegistry;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;
import com.gempukku.terasology.world.component.WorldComponent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

@RegisterSystem(
        profiles = NetProfiles.CLIENT)
public class RenderingChunkSystem implements EnvironmentRenderer, LifeCycleSystem {
    @In
    private EnvironmentRendererRegistry environmentRendererRegistry;
    @In
    private WorldStorage worldStorage;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private CommonBlockManager commonBlockManager;
    @In
    private ShapeProvider shapeProvider;
    @In
    private TerasologyComponentManager terasologyComponentManager;
    @In
    private ChunkBlocksProvider chunkBlocksProvider;

    private OfflineProcessingThread offlineProcessingThread;

    private ChunkRenderableBuilder chunkRenderableBuilder;

    private Multimap<String, RenderableChunk> renderableChunksInWorld = HashMultimap.create();

    private ModelBatch modelBatch;

    private volatile Camera lastCamera;

    @Override
    public void preInitialize() {
        modelBatch = new ModelBatch();
        offlineProcessingThread = new OfflineProcessingThread();
        Thread thr = new Thread(offlineProcessingThread);
        thr.start();
    }

    @Override
    public void initialize() {
        environmentRendererRegistry.registerEnvironmentRendered(this);
    }

    @Override
    public void postInitialize() {
    }

    @Override
    public void renderEnvironment(Camera camera, String worldId) {
        lastCamera = camera;
        synchronized (renderableChunksInWorld) {
            for (RenderableChunk renderableChunk : renderableChunksInWorld.values()) {
                renderableChunk.updateModelIfNeeded();
            }
        }

        modelBatch.begin(camera);

        synchronized (renderableChunksInWorld) {
            for (RenderableChunk renderableChunk : renderableChunksInWorld.get(worldId)) {
                if (renderableChunk.isRenderable() && renderableChunk.isVisible(camera)) {
                    modelBatch.render(renderableChunk.getRenderableProvider());
                }
            }
        }
        modelBatch.end();
    }

    @ReceiveEvent
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef worldEntity, WorldComponent worldComponent) {
        if (chunkRenderableBuilder == null) {
            initializeChunkRenderableBuilder();
        }

        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        synchronized (renderableChunksInWorld) {
            RenderableChunk renderableChunk = new RenderableChunk(chunkRenderableBuilder, worldId, x, y, z);
            renderableChunksInWorld.put(worldId, renderableChunk);

            for (RenderableChunk chunk : renderableChunksInWorld.get(worldId)) {
                if (chunk.getStatus().isInvalid() && canBeRenderedNow(worldId, chunk.x, chunk.y, chunk.z)) {
                    chunk.validate();
                }
            }
        }
    }

    @ReceiveEvent
    public void chunkUnloaded(BeforeChunkUnloadedEvent event, EntityRef worldEntity, WorldComponent worldComponent) {
        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        synchronized (renderableChunksInWorld) {
            Collection<RenderableChunk> renderableChunks = renderableChunksInWorld.get(worldId);
            RenderableChunk chunk = null;
            for (RenderableChunk renderableChunk : renderableChunks) {
                if (renderableChunk.x == x && renderableChunk.y == y && renderableChunk.z == z) {
                    chunk = renderableChunk;
                    break;
                }
            }
            if (chunk != null) {
                renderableChunks.remove(chunk);
                chunk.dispose();
            }
        }
    }

    private final int[][] surroundingChunks = new int[][]
            {
                    {-1, -1, -1}, {-1, -1, 0}, {-1, -1, 1},
                    {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1},
                    {-1, 1, -1}, {-1, 1, 0}, {-1, 1, 1},
                    {0, -1, -1}, {0, -1, 0}, {0, -1, 1},
                    {0, 0, -1}, /*{0, 0, 0},*/ {0, 0, 1},
                    {0, 1, -1}, {0, 1, 0}, {0, 1, 1},
                    {1, -1, -1}, {1, -1, 0}, {1, -1, 1},
                    {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
                    {1, 1, -1}, {1, 1, 0}, {1, 1, 1}
            };

    /**
     * Checks if all the chunks around it are loaded
     *
     * @param worldId
     * @param x
     * @param y
     * @param z
     * @return
     */
    private boolean canBeRenderedNow(String worldId, int x, int y, int z) {
        for (int[] surroundingChunk : surroundingChunks) {
            if (!worldStorage.hasChunk(worldId, x + surroundingChunk[0], y + surroundingChunk[1], z + surroundingChunk[2]))
                return false;
        }
        return true;
    }

    private void initializeChunkRenderableBuilder() {
        BlockMeshGenerator blockMeshGenerator = new BlockMeshGenerator(commonBlockManager, textureAtlasProvider,
                terasologyComponentManager, shapeProvider);
        chunkRenderableBuilder = new ChunkRenderableBuilder(blockMeshGenerator, chunkBlocksProvider, textureAtlasProvider.getTextureAtlas());
    }

    @Override
    public void postDestroy() {
        synchronized (renderableChunksInWorld) {
            for (RenderableChunk renderableChunk : renderableChunksInWorld.values()) {
                renderableChunk.dispose();
            }
        }

        modelBatch.dispose();
    }

    private class OfflineProcessingThread implements Runnable {
        public void run() {
            while (true) {
                RenderableChunk chunkToProcess = getClosestChunkToProcess();
                if (chunkToProcess == null) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException exp) {
                        // Ignore
                    }
                } else {
                    chunkToProcess.processOffLine();
                }
            }
        }

        private RenderableChunk getClosestChunkToProcess() {
            RenderableChunk chunkToProcess = null;
            if (lastCamera != null) {
                Vector3 cameraPosition = lastCamera.position;
                synchronized (renderableChunksInWorld) {
                    float minDistance = Float.MAX_VALUE;
                    for (RenderableChunk renderableChunk : renderableChunksInWorld.values()) {
                        if (renderableChunk.getStatus().canOfflineProcess()) {
                            float distance = cameraPosition.dst(
                                    renderableChunk.x * ChunkSize.X + ChunkSize.X / 2,
                                    renderableChunk.y * ChunkSize.Y + ChunkSize.Y / 2,
                                    renderableChunk.z * ChunkSize.Z + ChunkSize.Z / 2);
                            if (distance < minDistance) {
                                minDistance = distance;
                                chunkToProcess = renderableChunk;
                            }
                        }
                    }
                }
            }
            return chunkToProcess;
        }
    }
}
