package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;

public class CelestialBodyShaderProvider implements ShaderProvider {
    private CelestialBodyShader celestialBodyShader;

    private float viewportWidth;
    private float viewportHeight;

    public void prepareCelestialBodies(Iterable<CelestialBody> celestialBodies) {
        if (celestialBodyShader != null) {
            celestialBodyShader.prepareCelestialBodies(celestialBodies);
        }
    }

    public void setViewportWidth(float viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public void setViewportHeight(float viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public boolean hasBodiesToRender() {
        return celestialBodyShader == null || celestialBodyShader.hasBodiesToRender();
    }

    @Override
    public Shader getShader(Renderable renderable) {
        if (celestialBodyShader == null) {
            DefaultShader.Config config = new DefaultShader.Config(
                    Gdx.files.internal("shader/celestial.vert").readString(),
                    Gdx.files.internal("shader/celestial.frag").readString());
            config.defaultDepthFunc = 0;
            celestialBodyShader = new CelestialBodyShader(renderable,
                    config);
            celestialBodyShader.init();
        }
        celestialBodyShader.setViewportWidth(viewportWidth);
        celestialBodyShader.setViewportHeight(viewportHeight);
        return celestialBodyShader;
    }

    @Override
    public void dispose() {
        if (celestialBodyShader != null)
            celestialBodyShader.dispose();
    }
}
