package com.gempukku.terasology.particle.impl.selector.texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.impl.SimpleParticle;

public class ConstantParticleTextureSelector implements ParticleTextureSelector {
    private TextureRegion textureRegion;

    public ConstantParticleTextureSelector(TextureRegion textureRegion) {
        this.textureRegion = textureRegion;
    }

    @Override
    public TextureRegion getTextureRegion(SimpleParticle particle) {
        return textureRegion;
    }
}
