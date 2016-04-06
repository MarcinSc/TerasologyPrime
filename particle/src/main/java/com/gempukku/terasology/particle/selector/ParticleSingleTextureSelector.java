package com.gempukku.terasology.particle.selector;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.Particle;

public class ParticleSingleTextureSelector implements ParticleTextureSelector {
    private TextureRegion textureRegion;

    public ParticleSingleTextureSelector(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }

    @Override
    public TextureRegion getTextureRegion(Particle particle, float elapsedTime, float lifeLength) {
        return textureRegion;
    }
}
