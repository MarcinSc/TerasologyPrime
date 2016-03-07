package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Vector3;

public class ChunkLightShader extends DefaultShader {
    private final int u_cameraFar = register("u_cameraFar");
    private final int u_lightPosition = register("u_lightPosition");

    private Vector3 lightPosition;
    private float cameraFar;

    public ChunkLightShader(Renderable renderable, Config config) {
        super(renderable, config);
    }

    public void setCameraFar(float cameraFar) {
        this.cameraFar = cameraFar;
    }

    public void setLightPosition(Vector3 lightPosition) {
        this.lightPosition = lightPosition;
    }

    @Override
    public void render(Renderable renderable, Attributes combinedAttributes) {
        set(u_cameraFar, cameraFar);
        set(u_lightPosition, lightPosition);

        super.render(renderable, combinedAttributes);
    }
}
