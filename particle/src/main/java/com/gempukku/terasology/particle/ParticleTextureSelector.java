package com.gempukku.terasology.particle;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public interface ParticleTextureSelector {
    TextureRegion getTextureRegion(Particle particle);
}
