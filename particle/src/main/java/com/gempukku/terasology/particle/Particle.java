package com.gempukku.terasology.particle;

import com.badlogic.gdx.math.Vector3;

public class Particle {
    public final Vector3 location;
    public final float rotation;
    public final float scale;
    public final ParticleTextureSelector particleTextureSelector;

    public final Vector3 velocity;
    public final float lifeLength;

    public float elapsedTime;

    public Particle(Vector3 location, Vector3 velocity, float lifeLength, float rotation, float scale, ParticleTextureSelector particleTextureSelector) {
        this.location = location;
        this.velocity = velocity;
        this.lifeLength = lifeLength;
        this.rotation = rotation;
        this.scale = scale;
        this.particleTextureSelector = particleTextureSelector;
    }
}
