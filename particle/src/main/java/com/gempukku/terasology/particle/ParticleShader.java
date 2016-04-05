package com.gempukku.terasology.particle;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;

public class ParticleShader extends DefaultShader {
    private int u_viewportWidth = register("u_viewportWidth");
    private int u_viewportHeight = register("u_viewportHeight");

    public ParticleShader(Renderable renderable, Config config) {
        super(renderable, config);
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);

        set(u_viewportWidth, camera.viewportWidth);
        set(u_viewportHeight, camera.viewportHeight);
    }
}
