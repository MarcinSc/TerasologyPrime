package com.gempukku.terasology.particle.impl.selector.texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.impl.SimpleParticle;

public interface ParticleTextureSelector {
    TextureRegion getTextureRegion(SimpleParticle particle);
}
