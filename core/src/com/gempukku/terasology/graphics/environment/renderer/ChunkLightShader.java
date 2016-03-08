package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;

public class ChunkLightShader extends DefaultShader {
    private final int u_cameraFar = register("u_cameraFar");
    private final int u_lightPosition = register("u_lightPosition");
    private final int u_lightDirection = register("u_lightDirection");
    private final int u_lightPlaneDistance = register("u_lightPlaneDistance");

    private Vector3 lightPosition;
    private Vector3 lightDirection;
    private float lightPlaneDistance;
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

    public void setLightDirection(Vector3 lightDirection) {
        this.lightDirection = lightDirection;
    }

    public void setLightPlaneDistance(float lightPlaneDistance) {
        this.lightPlaneDistance = lightPlaneDistance;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);
        context.setDepthTest(GL20.GL_LEQUAL);
        context.setCullFace(GL20.GL_BACK);
    }

    @Override
    public void render(Renderable renderable, Attributes combinedAttributes) {
        set(u_cameraFar, cameraFar);
        set(u_lightPosition, lightPosition);
        set(u_lightDirection, lightDirection);
        set(u_lightPlaneDistance, lightPlaneDistance);

        super.render(renderable, combinedAttributes);
    }
}
