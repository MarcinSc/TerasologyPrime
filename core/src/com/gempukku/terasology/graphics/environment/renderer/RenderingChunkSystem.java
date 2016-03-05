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
import com.gempukku.terasology.world.chunk.ChunkComponent;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.chunk.event.BeforeChunkUnloadedEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    private Executor meshGenerationExecutor = Executors.newFixedThreadPool(5);

    private ChunkRenderableBuilder chunkRenderableBuilder;

    private Multimap<String, RenderableChunk> renderableChunksInWorld = Multimaps.synchronizedMultimap(HashMultimap.create());

    private Multimap<String, Vector3> loadedButNotRenderedChunks = HashMultimap.create();

    private ModelBatch modelBatch;

    @Override
    public void preInitialize() {
        modelBatch = new ModelBatch();
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
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef entity, ChunkComponent chunkComponent) {
        if (chunkRenderableBuilder == null) {
            initializeChunkRenderableBuilder();
        }

        String worldId = chunkComponent.getWorldId();
        int x = chunkComponent.getX();
        int y = chunkComponent.getY();
        int z = chunkComponent.getZ();

        loadedButNotRenderedChunks.put(worldId, new Vector3(x, y, z));

        for (Vector3 notRenderedChunk : new LinkedList<>(loadedButNotRenderedChunks.get(worldId))) {
            if (canBeRenderedNow(worldId, (int) notRenderedChunk.x, (int) notRenderedChunk.y, (int) notRenderedChunk.z)) {
                prepareRenderableChunk(worldId, (int) notRenderedChunk.x, (int) notRenderedChunk.y, (int) notRenderedChunk.z);
            }
        }
    }

    @ReceiveEvent
    public void chunkUnloaded(BeforeChunkUnloadedEvent event, EntityRef entity, ChunkComponent chunkComponent) {
        String worldId = chunkComponent.getWorldId();
        int x = chunkComponent.getX();
        int y = chunkComponent.getY();
        int z = chunkComponent.getZ();

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

        synchronized (loadedButNotRenderedChunks) {
            loadedButNotRenderedChunks.remove(worldId, new Vector3(x, y, z));
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

    private void prepareRenderableChunk(String worldId, int x, int y, int z) {
        RenderableChunk chunk = new RenderableChunk(chunkRenderableBuilder, worldId, x, y, z);
        renderableChunksInWorld.put(worldId, chunk);
        loadedButNotRenderedChunks.remove(worldId, new Vector3(x, y, z));
        meshGenerationExecutor.execute(new ChunkUpdateTask(chunk));
    }

    private void initializeChunkRenderableBuilder() {
        BlockMeshGenerator blockMeshGenerator = new BlockMeshGenerator(commonBlockManager, textureAtlasProvider,
                terasologyComponentManager, shapeProvider);
        chunkRenderableBuilder = new ChunkRenderableBuilder(blockMeshGenerator, chunkBlocksProvider, textureAtlasProvider.getTextureAtlas());
    }

    @Override
    public void postDestroy() {
        for (RenderableChunk renderableChunk : renderableChunksInWorld.values()) {
            renderableChunk.dispose();
        }

        modelBatch.dispose();
    }

    private class ChunkUpdateTask implements Runnable {
        private RenderableChunk chunk;

        public ChunkUpdateTask(RenderableChunk chunk) {
            this.chunk = chunk;
        }

        @Override
        public void run() {
            chunk.generateChunkLists();
        }
    }
}
