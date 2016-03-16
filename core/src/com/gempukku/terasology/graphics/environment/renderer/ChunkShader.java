package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class ChunkShader extends DefaultShader {
    private final int u_depthMap = register("u_depthMap");
    private final int u_lightTrans = register("u_lightTrans");
    private final int u_cameraFar = register("u_cameraFar");
    private final int u_lightDirection = register("u_lightDirection");
    private final int u_lightPosition = register("u_lightPosition");
    private final int u_lightPlaneDistance = register("u_lightPlaneDistance");
    private final int u_ambientLighting = register("u_ambientLighting");
    private final int u_fogColor = register("u_fogColor");
    private final int u_time = register("u_time");

    private Matrix4 lightTrans;
    private Vector3 lightDirection;
    private Vector3 lightPosition;
    private float lightPlaneDistance;
    private float cameraFar;
    private float time;

    public ChunkShader(Renderable renderable, Config config) {
        super(renderable, config);
    }

    public void setTime(float time) {
        this.time = time;
    }

    public void setLightTrans(Matrix4 lightTrans) {
        this.lightTrans = lightTrans;
    }

    public void setCameraFar(float cameraFar) {
        this.cameraFar = cameraFar;
    }

    public void setLightDirection(Vector3 lightDirection) {
        this.lightDirection = lightDirection;
    }

    public void setLightPlaneDistance(float lightPlaneDistance) {
        this.lightPlaneDistance = lightPlaneDistance;
    }

    public void setLightPosition(Vector3 lightPosition) {
        this.lightPosition = lightPosition;
    }

    @Override
    public void render(Renderable renderable, Attributes combinedAttributes) {
        set(u_lightTrans, lightTrans);
        set(u_cameraFar, cameraFar);
        set(u_lightPosition, lightPosition);
        set(u_lightDirection, lightDirection);
        set(u_lightPlaneDistance, lightPlaneDistance);
        set(u_depthMap, 2);
        set(u_ambientLighting, 0.5f);
        set(u_fogColor, new Vector3());
        set(u_time, time);
//        set(u_fogColor, new Vector3(145f/255, 186f/255, 220f/255));

        super.render(renderable, combinedAttributes);
    }
}
