package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
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
import com.google.common.collect.Multimap;

@RegisterSystem(
        profiles = NetProfiles.CLIENT)
public class ChunkRenderingSystem implements EnvironmentRenderer, LifeCycleSystem {
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
    private MyShaderProvider myShaderProvider;
    private FrameBuffer lightFrameBuffer;
    private Camera lightCamera;

    private static int shadowFidelity = 4;

    @Override
    public void preInitialize() {
        myShaderProvider = new MyShaderProvider();
        modelBatch = new ModelBatch(myShaderProvider);
        lightFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, shadowFidelity * 1024, shadowFidelity * 1024, true);
        lightCamera = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        renderLights(camera, worldId);
        renderChunks(camera, worldId);
    }

    private void renderLights(Camera camera, String worldId) {
        int dayLengthInMs = 1 * 60 * 1000;
        float direction = (float) (2 * Math.PI * (System.currentTimeMillis() % dayLengthInMs) / (1f * dayLengthInMs));

        lightCamera.position.set(
                (float) (camera.position.x + 1.1 * camera.far * Math.sin(direction)),
                (float) (camera.position.y + 1.1 * camera.far * Math.cos(direction)),
                camera.position.z);
        lightCamera.lookAt(camera.position.x, camera.position.y, camera.position.z);
        lightCamera.far = camera.far * 2.2f;
        lightCamera.near = camera.near;
        lightCamera.update();

        myShaderProvider.setShadowPass(true);
        myShaderProvider.setLightCameraFar(lightCamera.far);
        myShaderProvider.setLightPosition(lightCamera.position);
        myShaderProvider.setLightPlaneDistance(lightCamera.position.len());
        myShaderProvider.setLightDirection(lightCamera.direction);

        lightFrameBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(lightCamera);
        for (RenderableChunk renderableChunk : renderableChunksInWorld.get(worldId)) {
            if (renderableChunk.isRenderable()) {// && renderableChunk.isVisible(camera)) {
                modelBatch.render(renderableChunk.getRenderableProvider());
            }
        }
        modelBatch.end();
        lightFrameBuffer.end();
    }

    private void renderChunks(Camera camera, String worldId) {
        myShaderProvider.setShadowPass(false);
        myShaderProvider.setLightTrans(lightCamera.combined);
        myShaderProvider.setLightCameraFar(lightCamera.far);
        myShaderProvider.setLightPosition(lightCamera.position);
        myShaderProvider.setLightPlaneDistance(lightCamera.position.len());
        myShaderProvider.setLightDirection(lightCamera.direction);

        lightFrameBuffer.getColorBufferTexture().bind(2);

        modelBatch.begin(camera);

        for (RenderableChunk renderableChunk : renderableChunksInWorld.get(worldId)) {
            if (renderableChunk.isRenderable() && renderableChunk.isVisible(camera)) {
                modelBatch.render(renderableChunk.getRenderableProvider());
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
        renderableChunk.updateChunkMesh(chunkMeshManager.getChunkMesh(worldId, x, y, z), textureAtlasProvider.getTextures());
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
