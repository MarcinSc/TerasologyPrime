package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.environment.ChunkMeshManager;
import com.gempukku.terasology.graphics.environment.EnvironmentRenderer;
import com.gempukku.terasology.graphics.environment.EnvironmentRendererRegistry;
import com.gempukku.terasology.graphics.environment.event.AfterChunkMeshCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkMeshRemoved;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.component.WorldComponent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;

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
    @In
    private ChunkMeshManager chunkMeshManager;

    private Multimap<String, RenderableChunk> renderableChunksInWorld = HashMultimap.create();

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
    public void chunkMeshCreated(AfterChunkMeshCreated event, EntityRef worldEntity, WorldComponent worldComponent) {
        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        RenderableChunk renderableChunk = findRenderableChunk(worldId, x, y, z);
        if (renderableChunk == null) {
            renderableChunk = new RenderableChunk(worldId, x, y, z);
            renderableChunksInWorld.put(worldId, renderableChunk);
        }
        List<Texture> textures = new ArrayList<>();
        Iterables.addAll(textures, textureAtlasProvider.getTextureAtlas().getTextures());

        renderableChunk.updateChunkMesh(chunkMeshManager.getChunkMesh(worldId, x, y, z), textures);
    }

    @ReceiveEvent
    public void chunkMeshRemoved(BeforeChunkMeshRemoved event, EntityRef worldEntity, WorldComponent worldComponent) {
        String worldId = worldComponent.getWorldId();
        int x = event.x;
        int y = event.y;
        int z = event.z;

        RenderableChunk chunk = findRenderableChunk(worldId, x, y, z);
        if (chunk != null) {
            renderableChunksInWorld.remove(worldId, chunk);
        }
    }

    private RenderableChunk findRenderableChunk(String worldId, int x, int y, int z) {
        for (RenderableChunk renderableChunk : renderableChunksInWorld.get(worldId)) {
            if (renderableChunk.x == x && renderableChunk.y == y && renderableChunk.z == z)
                return renderableChunk;
        }
        return null;
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

    @Override
    public void postDestroy() {
        modelBatch.dispose();
    }
}
