package com.gempukku.terasology.graphics.backdrop;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.graphics.SkyColorProvider;

@RegisterSystem(
        profiles = NetProfiles.CLIENT)
public class BackgroundRenderer implements BackdropRenderer, LifeCycleSystem {
    @In
    private BackdropRendererRegistry backdropRendererRegistry;
    @In
    private SkyColorProvider skyColorProvider;

    private ModelBatch modelBatch;

    private BackgroundShaderProvider backgroundShaderProvider;
    private ModelInstance modelInstance;
    private Model model;

    @Override
    public void preInitialize() {
        backgroundShaderProvider = new BackgroundShaderProvider();

        modelBatch = new ModelBatch(backgroundShaderProvider);
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder backgroundBuilder = modelBuilder.part("background", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position, new Material());
        backgroundBuilder.rect(
                -1, 1, 1,
                -1, -1, 1,
                1, -1, 1,
                1, 1, 1,
                0, 0, 1);
        model = modelBuilder.end();

        modelInstance = new ModelInstance(model);
    }

    @Override
    public void initialize() {
        backdropRendererRegistry.registerBackdropRenderer(this);
    }

    @Override
    public void renderBackdrop(Camera camera, String worldId) {
        Vector3 skyColor = skyColorProvider.getSkyColor(
                worldId, camera.position.x, camera.position.y, camera.position.z);
        backgroundShaderProvider.setBackgroundColor(skyColor);

        modelBatch.begin(camera);
        modelBatch.render(modelInstance);
        modelBatch.end();
    }

    @Override
    public void postDestroy() {
        modelBatch.dispose();
        model.dispose();
    }
}
