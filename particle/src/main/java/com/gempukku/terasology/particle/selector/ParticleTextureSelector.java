package com.gempukku.terasology.particle.selector;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.Particle;

public interface ParticleTextureSelector {
    TextureRegion getTextureRegion(Particle particle, float elapsedTime, float lifeLength);
}
