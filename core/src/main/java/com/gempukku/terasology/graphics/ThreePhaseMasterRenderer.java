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
import com.gempukku.terasology.graphics.environment.renderer.MyShaderProvider;
import com.gempukku.terasology.graphics.ui.UiRenderer;
import com.gempukku.terasology.graphics.ui.UiRendererRegistry;
import com.gempukku.terasology.time.TimeManager;
import com.gempukku.terasology.world.component.LocationComponent;

@RegisterSystem(
        profiles = NetProfiles.CLIENT,
        shared = {RenderingEngine.class, EnvironmentRendererRegistry.class, UiRendererRegistry.class, BackdropRendererRegistry.class})
public class ThreePhaseMasterRenderer implements RenderingEngine, EnvironmentRendererRegistry, UiRendererRegistry, BackdropRendererRegistry,
        LifeCycleSystem {
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

    private PerspectiveCamera camera;
    private MyShaderProvider myShaderProvider;
    private ModelBatch modelBatch;
    private FrameBuffer lightFrameBuffer;
    private Camera lightCamera;

    private EntityIndex cameraAndLocationIndex;

    private static int shadowFidelity = 4;

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
    public void preInitialize() {
        camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShaderProvider = new MyShaderProvider();
        modelBatch = new ModelBatch(myShaderProvider);
        lightFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, shadowFidelity * 1024, shadowFidelity * 1024, true);
        lightCamera = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
    }

    @Override
    public void render() {
        //noinspection unchecked
        EntityRef activeCameraEntity = getActiveCameraEntity();

        if (activeCameraEntity != null) {
            String worldId = activeCameraEntity.getComponent(LocationComponent.class).getWorldId();

            // Number between 0 and 2*PI, where 0 is midnight, PI is midday
            float radialTimeOfDay = (float) (2 * Math.PI * timeManager.getWorldDayTime(worldId));

            setupCamera(activeCameraEntity);
            setupLight(radialTimeOfDay);

            setupShaders(worldId, radialTimeOfDay);

            cleanBuffer();

            renderBackdrop(worldId);

            renderEnvironment(worldId, radialTimeOfDay);
        }

        for (UiRenderer uiRenderer : uiRenderers) {
            uiRenderer.renderUi();
        }
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

    private void renderEnvironment(String worldId, float timeOfDay) {
        lightRenderPass(worldId, timeOfDay);

        normalRenderPass(worldId);
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
                (float) -(camera.position.x + 0.6 * camera.far * Math.sin(radialTimeOfDay)),
                (float) -(camera.position.y + 0.6 * camera.far * Math.cos(radialTimeOfDay)),
                camera.position.z);
        if (radialTimeOfDay == 0) {
            lightCamera.up.set(1, 0, 0);
        } else {
            lightCamera.up.set(0, 1, 0);
        }
        lightCamera.lookAt(camera.position.x, camera.position.y, camera.position.z);
        lightCamera.far = camera.far * 1.21f;
        lightCamera.near = camera.near;
        lightCamera.update();
    }

    private void lightRenderPass(String worldId, float radialTimeOfDay) {
        lightFrameBuffer.begin();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

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
        lightFrameBuffer.end();
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
}
