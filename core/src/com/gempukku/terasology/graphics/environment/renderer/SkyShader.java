package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.math.Vector3;

public class SkyShader extends DefaultShader {
    private final int u_skyColor = register("u_skyColor");
    private final int u_lightDirection = register("u_lightDirection");

    private Vector3 lightDirection;
    private Vector3 skyColor;

    public SkyShader(Renderable renderable, Config config) {
        super(renderable, config);
    }

    public void setSkyColor(Vector3 skyColor) {
        this.skyColor = skyColor;
    }

    public void setLightDirection(Vector3 lightDirection) {
        this.lightDirection = lightDirection;
    }

    @Override
    public void render(Renderable renderable, Attributes combinedAttributes) {
        set(u_skyColor, skyColor);
        set(u_lightDirection, lightDirection);

        super.render(renderable, combinedAttributes);
    }
}
