package com.gempukku.terasology.particle;

public interface ParticleEmitter {
    String PARTICLES_ATLAS_NAME = "particles";

    void emitParticle(Particle particle);
}
