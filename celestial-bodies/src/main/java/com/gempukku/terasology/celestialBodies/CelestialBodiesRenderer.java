package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.context.util.Prioritable;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;
import com.gempukku.terasology.graphics.backdrop.BackdropRenderer;
import com.gempukku.terasology.graphics.backdrop.BackdropRendererRegistry;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = CelestialBodyTypeRendererRegistry.class)
public class CelestialBodiesRenderer implements BackdropRenderer, LifeCycleSystem,
        CelestialBodyTypeRendererRegistry, Prioritable {
    @In
    private BackdropRendererRegistry backdropRendererRegistry;

    private CelestialBodyShaderProvider shaderProvider;
    private ModelBatch modelBatch;
    private Model model;
    private ModelInstance modelInstance;
    private List<CelestialBodyTypeRenderer> bodyTypeRenderers = new ArrayList<>();

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public int registerCelestialBodyTypeRenderer(CelestialBodyTypeRenderer celestialBodyTypeRenderer) {
        bodyTypeRenderers.add(celestialBodyTypeRenderer);
        return bodyTypeRenderers.size() - 1;
    }

    @Override
    public void preInitialize() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder backgroundBuilder = modelBuilder.part("background", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position,
                new Material(new BlendingAttribute()));
        backgroundBuilder.rect(
                -1, -1, 1,
                1, -1, 1,
                1, 1, 1,
                -1, 1, 1,
                0, 0, 1);
        model = modelBuilder.end();

        modelInstance = new ModelInstance(model);
    }

    @Override
    public void postInitialize() {
        shaderProvider = new CelestialBodyShaderProvider(bodyTypeRenderers);
        modelBatch = new ModelBatch(shaderProvider);
    }

    @Override
    public void initialize() {
        backdropRendererRegistry.registerBackdropRenderer(this);
    }

    @Override
    public void renderBackdrop(Camera camera, String worldId) {
        List<CelestialBody> visibleCelestialBodies = new LinkedList<>();
        for (CelestialBodyTypeRenderer bodyTypeRenderer : bodyTypeRenderers) {
            Iterables.addAll(visibleCelestialBodies, bodyTypeRenderer.getCelestialBodies(
                    worldId, camera.position.x, camera.position.y, camera.position.z));
        }
        shaderProvider.prepareCelestialBodies(visibleCelestialBodies);
        while (shaderProvider.hasBodiesToRender()) {
            modelBatch.begin(camera);
            modelBatch.render(modelInstance);
            modelBatch.end();
        }
    }

    @Override
    public void postDestroy() {
        modelBatch.dispose();
        model.dispose();
    }
}
