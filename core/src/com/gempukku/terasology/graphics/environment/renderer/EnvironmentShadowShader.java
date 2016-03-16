package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Vector3;

public class EnvironmentShadowShader extends DefaultShader {
    private final int u_cameraFar = register("u_cameraFar");
    private final int u_lightPosition = register("u_lightPosition");
    private final int u_lightDirection = register("u_lightDirection");
    private final int u_lightPlaneDistance = register("u_lightPlaneDistance");
    private final int u_time = register("u_time");

    private Vector3 lightPosition;
    private Vector3 lightDirection;
    private float lightPlaneDistance;
    private float cameraFar;
    private float time;

    public EnvironmentShadowShader(Renderable renderable, Config config) {
        super(renderable, config);
    }

    public void setTime(float time) {
        this.time = time;
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
    public void render(Renderable renderable, Attributes combinedAttributes) {
        set(u_cameraFar, cameraFar);
        set(u_lightPosition, lightPosition);
        set(u_lightDirection, lightDirection);
        set(u_lightPlaneDistance, lightPlaneDistance);
        set(u_time, time);

        super.render(renderable, combinedAttributes);
    }
}
