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
        shared = {RenderingEngine.class, EnvironmentRendererRegistry.class, UiRendererRegistry.class})
public class TwoPhaseRenderingEngine implements RenderingEngine, EnvironmentRendererRegistry, UiRendererRegistry,
        LifeCycleSystem {
    @In
    private EntityManager entityManager;
    @In
    private TimeManager timeManager;

    private PriorityCollection<EnvironmentRenderer> environmentRenderers = new PriorityCollection<>();
    private PriorityCollection<UiRenderer> uiRenderers = new PriorityCollection<>();

    private PerspectiveCamera camera;
    private MyShaderProvider myShaderProvider;
    private ModelBatch modelBatch;
    private FrameBuffer lightFrameBuffer;
    private Camera lightCamera;

    private ModelInstance skySphere;

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
        if (skySphere != null)
            skySphere.model.dispose();
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
            float timeOfDay = (float) (2 * Math.PI * (timeManager.getMultiverseTime() % dayLengthInMs) / (1f * dayLengthInMs));

            setupCamera(activeCameraEntity);
            setupLight(timeOfDay);

            setupShaders(timeOfDay);

            cleanBuffer();

            renderSky();
            renderEnvironment(activeCameraEntity, timeOfDay);
        }

        for (UiRenderer uiRenderer : uiRenderers) {
            uiRenderer.renderUi();
        }
    }

    private void setupShaders(float timeOfDay) {
        Vector3 skyColor = new Vector3(145f / 255, 186f / 255, 220f / 255);
        float dayComponent = (float) Math.cos(timeOfDay);
        if (dayComponent < -0.3f) {
            skyColor.scl(0);
        } else if (dayComponent < 0.3) {
            skyColor.scl((dayComponent + 0.3f) / (2f * 0.3f));
        }

        myShaderProvider.setSkyColor(skyColor);
        // Time used in shading depends on real time, not multiverse time
        myShaderProvider.setTime((System.currentTimeMillis() % 10000) / 1000f);
        myShaderProvider.setLightTrans(lightCamera.combined);
        myShaderProvider.setLightPosition(lightCamera.position);
        myShaderProvider.setLightPlaneDistance(lightCamera.position.len());
        myShaderProvider.setLightDirection(lightCamera.direction);
    }

    private void cleanBuffer() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }

    private void renderSky() {
        if (skySphere == null)
            initSkySphere();

        skySphere.transform = new Matrix4().translate(
                camera.position.x, camera.position.y, camera.position.z).scale(-1, -1, -1);

        myShaderProvider.setMode(MyShaderProvider.Mode.SKY);
        modelBatch.begin(camera);
        modelBatch.render(skySphere);
        modelBatch.end();
    }

    private void initSkySphere() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        Material material = new Material();
        MeshPartBuilder skyBuilder = modelBuilder.part("sky", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, material);

        // The diameter is 2 * maximum camera view distance, we have to decrement it a bit, otherwise we might
        // be seeing some see-through artifacts on the sphere due to far distance culling
        float skyDiameter = 2 * camera.far - 0.2f;
        skyBuilder.sphere(skyDiameter, skyDiameter, skyDiameter, 128, 128);
        Model model = modelBuilder.end();
        skySphere = new ModelInstance(model);
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
        if (timeOfDay == 0) {
            lightCamera.up.set(1, 0, 0);
        } else {
            lightCamera.up.set(0, 1, 0);
        }
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
