package com.gempukku.terasology.particle.selector;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.Particle;
import com.gempukku.terasology.particle.ParticleTextureSelector;

public class SingleParticleTextureSelector implements ParticleTextureSelector {
    private TextureRegion textureRegion;

    public SingleParticleTextureSelector(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }

    @Override
    public TextureRegion getTextureRegion(Particle particle) {
        return textureRegion;
    }
}
