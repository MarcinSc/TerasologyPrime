package com.gempukku.terasology.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
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

        //noinspection unchecked
        EntityRef activeCameraEntity = getActiveCameraEntity();

        if (activeCameraEntity != null) {
            int dayLengthInMs = 1 * 60 * 1000;

            // Number between 0 and 2*PI, where 0 is "midday", PI is midnight
            float timeOfDay = (float) (2 * Math.PI * (System.currentTimeMillis() % dayLengthInMs) / (1f * dayLengthInMs));

            setupCamera(activeCameraEntity);
            setupLight(timeOfDay);

            setupShaders();

            cleanBuffer();

            renderSky();
            renderEnvironment(activeCameraEntity, timeOfDay);
        }

        for (UiRenderer uiRenderer : uiRenderers) {
            uiRenderer.renderUi();
        }
    }

    private void setupShaders() {
        myShaderProvider.setSkyColor(new Vector3(145f / 255, 186f / 255, 220f / 255));
        myShaderProvider.setTime((System.currentTimeMillis() % 10000) / 1000f);
        myShaderProvider.setLightTrans(lightCamera.combined);
        myShaderProvider.setLightCameraFar(lightCamera.far);
        myShaderProvider.setLightPosition(lightCamera.position);
        myShaderProvider.setLightPlaneDistance(lightCamera.position.len());
        myShaderProvider.setLightDirection(lightCamera.direction);
    }

    private void cleanBuffer() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    private void renderSky() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        Material material = new Material();
        MeshPartBuilder skyBuilder = modelBuilder.part("sky", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, material);
        skyBuilder.sphere(1024, 1024, 1024, 64, 64);
        Model model = modelBuilder.end();
        ModelInstance modelInstance = new ModelInstance(model);
        modelInstance.transform = new Matrix4().translate(
                camera.position.x, camera.position.y, camera.position.z).scale(-1, -1, -1);

        float oldFar = camera.far;

        camera.far = 2000;
        camera.update();

        myShaderProvider.setMode(MyShaderProvider.Mode.SKY);
        modelBatch.begin(camera);
        modelBatch.render(modelInstance);
        modelBatch.end();

        model.dispose();

        camera.far = oldFar;
        camera.update();
    }

    private EntityRef getActiveCameraEntity() {
        EntityRef activeCameraEntity = null;
        for (EntityRef entity : entityManager.getEntitiesWithComponents(CameraComponent.class, LocationComponent.class)) {
            CameraComponent cameraComponent = entity.getComponent(CameraComponent.class);
            if (cameraComponent.isActive()) {
                activeCameraEntity = entity;
                break;
            }
        }
        return activeCameraEntity;
    }

    private void renderEnvironment(EntityRef activeCameraEntity, float timeOfDay) {
        String worldId = activeCameraEntity.getComponent(LocationComponent.class).getWorldId();

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

    private void setupLight(float timeOfDay) {
        lightCamera.position.set(
                (float) (camera.position.x + 1.1 * camera.far * Math.sin(timeOfDay)),
                (float) (camera.position.y + 1.1 * camera.far * Math.cos(timeOfDay)),
                camera.position.z);
        lightCamera.lookAt(camera.position.x, camera.position.y, camera.position.z);
        lightCamera.far = camera.far * 2.2f;
        lightCamera.near = camera.near;
        lightCamera.update();
    }

    private void lightRenderPass(String worldId, float timeOfDay) {
        lightFrameBuffer.begin();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // If sun is over the horizon, just skip drawing anything in the light pass to save time
        // (and avoid artifacts created due to light shining through from beneath the chunks)
        boolean day = timeOfDay < Math.PI / 2f || timeOfDay > 3 * Math.PI / 2f;
        if (day) {
            myShaderProvider.setMode(MyShaderProvider.Mode.ENVIRONMENT_SHADOW);
            modelBatch.begin(lightCamera);
            for (EnvironmentRenderer environmentRenderer : environmentRenderers) {
                environmentRenderer.renderEnvironment(lightCamera, worldId, modelBatch);
            }
            modelBatch.end();
        }
        lightFrameBuffer.end();
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
