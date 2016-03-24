package com.gempukku.terasology.graphics;

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
import com.gempukku.secsy.context.util.PriorityCollection;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.terasology.graphics.backdrop.BackdropRenderer;
import com.gempukku.terasology.graphics.backdrop.BackdropRendererRegistry;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.graphics.environment.EnvironmentRenderer;
import com.gempukku.terasology.graphics.environment.EnvironmentRendererRegistry;
import com.gempukku.terasology.graphics.environment.RenderingBuffer;
import com.gempukku.terasology.graphics.environment.renderer.MyShaderProvider;
import com.gempukku.terasology.graphics.postprocess.PostProcessingRenderer;
import com.gempukku.terasology.graphics.postprocess.PostProcessingRendererRegistry;
import com.gempukku.terasology.graphics.ui.UiRenderer;
import com.gempukku.terasology.graphics.ui.UiRendererRegistry;
import com.gempukku.terasology.time.TimeManager;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = NetProfiles.CLIENT,
        shared = {RenderingEngine.class, EnvironmentRendererRegistry.class, UiRendererRegistry.class, BackdropRendererRegistry.class,
                PostProcessingRendererRegistry.class})
public class FourPhaseMasterRenderer implements RenderingEngine, EnvironmentRendererRegistry, UiRendererRegistry, BackdropRendererRegistry,
        PostProcessingRendererRegistry, LifeCycleSystem {
    @In
    private EntityManager entityManager;
    @In
    private EntityIndexManager entityIndexManager;
    @In
    private TimeManager timeManager;
    @In
    private SkyColorProvider skyColorProvider;

    private PriorityCollection<BackdropRenderer> backdropRenderers = new PriorityCollection<>();
    private PriorityCollection<EnvironmentRenderer> environmentRenderers = new PriorityCollection<>();
    private PriorityCollection<UiRenderer> uiRenderers = new PriorityCollection<>();
    private PriorityCollection<PostProcessingRenderer> postProcessingRenderers = new PriorityCollection<>();

    private PerspectiveCamera camera;
    private MyShaderProvider myShaderProvider;
    private ModelBatch modelBatch;
    private FrameBuffer lightFrameBuffer;
    private Camera lightCamera;

    private EntityIndex cameraAndLocationIndex;

    private static int shadowFidelity = 4;

    private FrameBuffer firstOffScreenBuffer;
    private FrameBuffer secondOffScreenBuffer;

    private RenderingBuffer screenRenderingBuffer;
    private RenderingBuffer lightsRenderingBuffer;

    @Override
    public void registerEnvironmentRendered(EnvironmentRenderer environmentRenderer) {
        environmentRenderers.add(environmentRenderer);
    }

    @Override
    public void registerUiRenderer(UiRenderer uiRenderer) {
        uiRenderers.add(uiRenderer);
    }

    @Override
    public void registerBackdropRenderer(BackdropRenderer backdropRenderer) {
        backdropRenderers.add(backdropRenderer);
    }

    @Override
    public void registerPostProcessingRenderer(PostProcessingRenderer postProcessingRenderer) {
        postProcessingRenderers.add(postProcessingRenderer);
    }

    @Override
    public void preInitialize() {
        updateCamera();
        myShaderProvider = new MyShaderProvider();
        modelBatch = new ModelBatch(myShaderProvider);
        lightFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, shadowFidelity * 1024, shadowFidelity * 1024, true);
        lightCamera = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        screenRenderingBuffer = new ScreenRenderingBuffer();
        lightsRenderingBuffer = new OffScreenRenderingBuffer(lightFrameBuffer);
    }

    @Override
    public void initialize() {
        cameraAndLocationIndex = entityIndexManager.addIndexOnComponents(CameraComponent.class, LocationComponent.class);
    }

    @Override
    public void postDestroy() {
        lightFrameBuffer.dispose();
        modelBatch.dispose();
    }

    @Override
    public void updateCamera() {
        camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (firstOffScreenBuffer != null) {
            firstOffScreenBuffer.dispose();
        }
        if (secondOffScreenBuffer != null) {
            secondOffScreenBuffer.dispose();
        }
        firstOffScreenBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        secondOffScreenBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    @Override
    public void render() {
        //noinspection unchecked
        EntityRef activeCameraEntity = getActiveCameraEntity();

        if (activeCameraEntity != null) {
            String worldId = activeCameraEntity.getComponent(LocationComponent.class).getWorldId();
            Collection<PostProcessingRenderer> enabledPostProcessors = getEnabledPostProcessors(activeCameraEntity);

            // Number between 0 and 2*PI, where 0 is midnight, PI is midday
            float radialTimeOfDay = (float) (2 * Math.PI * timeManager.getWorldDayTime(worldId));

            setupCamera(activeCameraEntity);
            setupLight(radialTimeOfDay);

            setupShaders(worldId, radialTimeOfDay);

            renderLightMap(worldId, radialTimeOfDay);

            RenderingBuffer mainPassBuffer;
            if (enabledPostProcessors.isEmpty()) {
                mainPassBuffer = screenRenderingBuffer;
            } else {
                mainPassBuffer = new OffScreenRenderingBuffer(firstOffScreenBuffer);
            }

            renderCameraView(worldId, mainPassBuffer);

            if (!enabledPostProcessors.isEmpty()) {
                postProcess(activeCameraEntity, enabledPostProcessors);
            }
        }

        for (UiRenderer uiRenderer : uiRenderers) {
            uiRenderer.renderUi();
        }
    }

    private void postProcess(EntityRef observerEntity, Collection<PostProcessingRenderer> enabledPostProcessors) {
        FlipOffScreenRenderingBuffer buffer = new FlipOffScreenRenderingBuffer(firstOffScreenBuffer, secondOffScreenBuffer);
        Iterator<PostProcessingRenderer> iterator = enabledPostProcessors.iterator();
        while (iterator.hasNext()) {
            PostProcessingRenderer postProcessor = iterator.next();

            boolean hasNext = iterator.hasNext();
            RenderingBuffer resultBuffer = hasNext ? buffer : screenRenderingBuffer;

            buffer.flip();
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + 2);
            Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, buffer.getSourceBuffer().getColorBufferTexture().getTextureObjectHandle());
//            buffer.getSourceBuffer().getColorBufferTexture().bind(2);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + 3);
            Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, buffer.getSourceBuffer().getDepthBufferHandle());

            postProcessor.render(observerEntity, resultBuffer, camera, 2, 3);
        }
    }

    private void renderCameraView(String worldId, RenderingBuffer mainPassBuffer) {
        mainPassBuffer.begin();
        cleanBuffer();
        renderBackdrop(worldId);
        normalRenderPass(worldId);
        mainPassBuffer.end();
    }

    private void renderLightMap(String worldId, float radialTimeOfDay) {
        lightsRenderingBuffer.begin();
        cleanBuffer();
        lightRenderPass(worldId, radialTimeOfDay);
        lightsRenderingBuffer.end();
    }

    private Collection<PostProcessingRenderer> getEnabledPostProcessors(EntityRef activeCameraEntity) {
        List<PostProcessingRenderer> renderers = new LinkedList<>();
        for (PostProcessingRenderer postProcessingRenderer : postProcessingRenderers) {
            if (postProcessingRenderer.isEnabled(activeCameraEntity))
                renderers.add(postProcessingRenderer);
        }

        return renderers;
    }

    private void setupShaders(String worldId, float radialTimeOfDay) {
        myShaderProvider.setSkyColor(skyColorProvider.getSkyColor(
                worldId, camera.position.x, camera.position.y, camera.position.z));
        float ambientLight = getAmbientLight(radialTimeOfDay);

        myShaderProvider.setAmbientLight(ambientLight);
        // Time used in shading depends on real time, not multiverse time
        myShaderProvider.setTime((System.currentTimeMillis() % 10000) / 1000f);
        myShaderProvider.setLightTrans(lightCamera.combined);
        myShaderProvider.setLightPosition(lightCamera.position);
        myShaderProvider.setLightPlaneDistance(lightCamera.position.len());
        myShaderProvider.setLightDirection(lightCamera.direction);
        myShaderProvider.setNight(!isDay(radialTimeOfDay));
        myShaderProvider.setShadowMapSize(shadowFidelity * 1024);
    }

    private float getAmbientLight(float radialTimeOfDay) {
        float ambientLight = 0.4f;

        float dayComponent = (float) -Math.cos(radialTimeOfDay);
        if (dayComponent < -0.3f) {
            ambientLight = 0.2f;
        } else if (dayComponent < 0.3) {
            ambientLight = 0.2f + 0.2f * (dayComponent + 0.3f) / (2f * 0.3f);
        }
        return ambientLight;
    }

    private void cleanBuffer() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    private void renderBackdrop(String worldId) {
        for (BackdropRenderer backdropRenderer : backdropRenderers) {
            backdropRenderer.renderBackdrop(camera, worldId);
        }
    }

    private EntityRef getActiveCameraEntity() {
        EntityRef activeCameraEntity = null;
        for (EntityRef entity : cameraAndLocationIndex.getEntities()) {
            CameraComponent cameraComponent = entity.getComponent(CameraComponent.class);
            if (cameraComponent.isActive()) {
                activeCameraEntity = entity;
                break;
            }
        }
        return activeCameraEntity;
    }

    private void setupCamera(EntityRef activeCameraEntity) {
        CameraComponent cameraComponent = activeCameraEntity.getComponent(CameraComponent.class);

        camera.near = cameraComponent.getNear();
        camera.far = cameraComponent.getFar();

        LocationComponent location = activeCameraEntity.getComponent(LocationComponent.class);
        camera.position.set(
                location.getX() + cameraComponent.getTranslateFromLocationX(),
                location.getY() + cameraComponent.getTranslateFromLocationY(),
                location.getZ() + cameraComponent.getTranslateFromLocationZ());

        camera.direction.set(cameraComponent.getDirectionX(), cameraComponent.getDirectionY(), cameraComponent.getDirectionZ());
        camera.up.set(0, 1, 0);

        camera.update();
    }

    private void setupLight(float radialTimeOfDay) {
        lightCamera.position.set(
                (float) -(camera.position.x + 1.1 * camera.far * Math.sin(radialTimeOfDay)),
                (float) -(camera.position.y + 1.1 * camera.far * Math.cos(radialTimeOfDay)),
                camera.position.z);
        if (radialTimeOfDay == 0) {
            lightCamera.up.set(1, 0, 0);
        } else {
            lightCamera.up.set(0, 1, 0);
        }
        lightCamera.lookAt(camera.position.x, camera.position.y, camera.position.z);
        lightCamera.far = camera.far * 2.21f;
        lightCamera.near = camera.near;
        lightCamera.update();
    }

    private void lightRenderPass(String worldId, float radialTimeOfDay) {
        // If sun is over the horizon, just skip drawing anything in the light pass to save time
        // (and avoid artifacts created due to light shining through from beneath the chunks)
        if (isDay(radialTimeOfDay)) {
            myShaderProvider.setMode(MyShaderProvider.Mode.ENVIRONMENT_SHADOW);
            modelBatch.begin(lightCamera);
            for (EnvironmentRenderer environmentRenderer : environmentRenderers) {
                environmentRenderer.renderEnvironment(lightCamera, worldId, modelBatch);
            }
            modelBatch.end();
        }
    }

    private boolean isDay(float radialTimeOfDay) {
        return radialTimeOfDay > Math.PI / 2f && radialTimeOfDay < 3 * Math.PI / 2f;
    }

    private void normalRenderPass(String worldId) {
        myShaderProvider.setMode(MyShaderProvider.Mode.ENVIRONMENT);
        lightFrameBuffer.getColorBufferTexture().bind(2);
        modelBatch.begin(camera);
        for (EnvironmentRenderer environmentRenderer : environmentRenderers) {
            environmentRenderer.renderEnvironment(camera, worldId, modelBatch);
        }
        modelBatch.end();
    }

    private class ScreenRenderingBuffer implements RenderingBuffer {
        @Override
        public void begin() {

        }

        @Override
        public void end() {

        }
    }

    private class OffScreenRenderingBuffer implements RenderingBuffer {
        private FrameBuffer frameBuffer;

        public OffScreenRenderingBuffer(FrameBuffer frameBuffer) {
            this.frameBuffer = frameBuffer;
        }

        @Override
        public void begin() {
            frameBuffer.begin();
        }

        @Override
        public void end() {
            frameBuffer.end();
        }
    }

    private class FlipOffScreenRenderingBuffer implements RenderingBuffer {
        private FrameBuffer firstFrameBuffer;
        private FrameBuffer secondFrameBuffer;
        private boolean drawsToFirst = true;

        public FlipOffScreenRenderingBuffer(FrameBuffer firstFrameBuffer, FrameBuffer secondFrameBuffer) {
            this.firstFrameBuffer = firstFrameBuffer;
            this.secondFrameBuffer = secondFrameBuffer;
        }

        @Override
        public void begin() {

        }

        @Override
        public void end() {

        }

        public void flip() {
            drawsToFirst = !drawsToFirst;
        }

        public FrameBuffer getSourceBuffer() {
            if (drawsToFirst)
                return secondFrameBuffer;
            else
                return firstFrameBuffer;
        }
    }
}
