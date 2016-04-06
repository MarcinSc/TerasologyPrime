package com.gempukku.terasology.particle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.math.Vector3;

public class ParticleShaderProvider implements ShaderProvider {
    private ParticleShader particleShader;

    private Vector3 fogColor;

    public void setFogColor(Vector3 fogColor) {
        this.fogColor = fogColor;
    }

    @Override
    public Shader getShader(Renderable renderable) {
        if (particleShader == null) {
            DefaultShader.Config config = new DefaultShader.Config(
                    Gdx.files.internal("shader/particle.vert").readString(),
                    Gdx.files.internal("shader/particle.frag").readString());
            particleShader = new ParticleShader(renderable, config);
            particleShader.init();
        }
        particleShader.setFogColor(fogColor);
        return particleShader;
    }

    @Override
    public void dispose() {
        if (particleShader != null)
            particleShader.dispose();
    }
}
