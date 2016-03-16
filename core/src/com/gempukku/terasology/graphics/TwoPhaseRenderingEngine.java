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
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.graphics.environment.EnvironmentRenderer;
import com.gempukku.terasology.graphics.environment.EnvironmentRendererRegistry;
import com.gempukku.terasology.graphics.environment.renderer.MyShaderProvider;
import com.gempukku.terasology.graphics.ui.UiRenderer;
import com.gempukku.terasology.graphics.ui.UiRendererRegistry;

@RegisterSystem(
        profiles = NetProfiles.CLIENT,
        shared = {RenderingEngine.class, EnvironmentRendererRegistry.class, UiRendererRegistry.class})
public class TwoPhaseRenderingEngine implements RenderingEngine, EnvironmentRendererRegistry, UiRendererRegistry,
        LifeCycleSystem {
    @In
    private EntityManager entityManager;

    private PriorityCollection<EnvironmentRenderer> environmentRenderers = new PriorityCollection<>();
    private PriorityCollection<UiRenderer> uiRenderers = new PriorityCollection<>();

    private PerspectiveCamera camera;
    private MyShaderProvider myShaderProvider;
    private ModelBatch modelBatch;
    private FrameBuffer lightFrameBuffer;
    private Camera lightCamera;

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
    public void preInitialize() {
        camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        myShaderProvider = new MyShaderProvider();
        modelBatch = new ModelBatch(myShaderProvider);
        lightFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, shadowFidelity * 1024, shadowFidelity * 1024, true);
        lightCamera = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        String worldId = null;
        boolean hasActiveCamera = false;

        //noinspection unchecked
        for (EntityRef entity : entityManager.getEntitiesWithComponents(CameraComponent.class, LocationComponent.class)) {
            CameraComponent cameraComponent = entity.getComponent(CameraComponent.class);
            if (cameraComponent.isActive()) {
                camera.near = cameraComponent.getNear();
                camera.far = cameraComponent.getFar();

                LocationComponent location = entity.getComponent(LocationComponent.class);
                camera.position.set(
                        location.getX() + cameraComponent.getTranslateFromLocationX(),
                        location.getY() + cameraComponent.getTranslateFromLocationY(),
                        location.getZ() + cameraComponent.getTranslateFromLocationZ());

                camera.direction.set(cameraComponent.getDirectionX(), cameraComponent.getDirectionY(), cameraComponent.getDirectionZ());
                camera.up.set(0, 1, 0);

                camera.update();

                worldId = location.getWorldId();
                hasActiveCamera = true;

                break;
            }
        }

        if (hasActiveCamera) {
            renderEnvironment(worldId);
        }

        for (UiRenderer uiRenderer : uiRenderers) {
            uiRenderer.renderUi();
        }
    }

    private void renderEnvironment(String worldId) {
        // Number between 0-2*PI, where 0 is "midday", PI is midnight
        float timeOfDay = setupLight();

        myShaderProvider.setTime((System.currentTimeMillis() % 10000) / 1000f);
        myShaderProvider.setLightTrans(lightCamera.combined);
        myShaderProvider.setLightCameraFar(lightCamera.far);
        myShaderProvider.setLightPosition(lightCamera.position);
        myShaderProvider.setLightPlaneDistance(lightCamera.position.len());
        myShaderProvider.setLightDirection(lightCamera.direction);

        lightRenderPass(worldId, timeOfDay);

        normalRenderPass(worldId);
    }

    private float setupLight() {
        int dayLengthInMs = 1 * 60 * 1000;
        float timeOfDay = (float) (2 * Math.PI * (System.currentTimeMillis() % dayLengthInMs) / (1f * dayLengthInMs));

        lightCamera.position.set(
                (float) (camera.position.x + 1.1 * camera.far * Math.sin(timeOfDay)),
                (float) (camera.position.y + 1.1 * camera.far * Math.cos(timeOfDay)),
                camera.position.z);
        lightCamera.lookAt(camera.position.x, camera.position.y, camera.position.z);
        lightCamera.far = camera.far * 2.2f;
        lightCamera.near = camera.near;
        lightCamera.update();
        return timeOfDay;
    }

    private void lightRenderPass(String worldId, float timeOfDay) {
        lightFrameBuffer.begin();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // If sun is over the horizon, just skip drawing anything in the light pass to save time
        // (and avoid artifacts created due to light shining through from beneath the chunks)
        boolean day = timeOfDay < Math.PI / 2f || timeOfDay > 3 * Math.PI / 2f;
        if (day) {
            myShaderProvider.setShadowPass(true);
            modelBatch.begin(lightCamera);
            for (EnvironmentRenderer environmentRenderer : environmentRenderers) {
                environmentRenderer.renderEnvironment(lightCamera, worldId, modelBatch);
            }
            modelBatch.end();
        }
        lightFrameBuffer.end();
    }

    private void normalRenderPass(String worldId) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        myShaderProvider.setShadowPass(false);
        lightFrameBuffer.getColorBufferTexture().bind(2);
        modelBatch.begin(camera);
        for (EnvironmentRenderer environmentRenderer : environmentRenderers) {
            environmentRenderer.renderEnvironment(camera, worldId, modelBatch);
        }
        modelBatch.end();
    }
}
