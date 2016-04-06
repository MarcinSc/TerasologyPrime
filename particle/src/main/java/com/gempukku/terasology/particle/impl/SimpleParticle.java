package com.gempukku.terasology.particle.impl;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.terasology.particle.Particle;
import com.gempukku.terasology.particle.selector.ParticleTextureSelector;

public class SimpleParticle implements Particle {
    public final String worldId;
    public final Vector3 location;
    public float rotation;
    public float scale;
    public final ParticleTextureSelector particleTextureSelector;

    public final Vector3 velocity;
    public final float gravityInfluence;
    public final float rotationVelocity;
    public final float scaleDiff;
    public final float lifeLength;

    public float elapsedTime;

    public SimpleParticle(String worldId, Vector3 location, Vector3 velocity, float gravityInfluence, float rotation, float rotationVelocity, float scale, float scaleDiff, float lifeLength, ParticleTextureSelector particleTextureSelector) {
        this.worldId = worldId;
        this.location = location;
        this.velocity = velocity;
        this.gravityInfluence = gravityInfluence;
        this.rotation = rotation;
        this.rotationVelocity = rotationVelocity;
        this.scale = scale;
        this.scaleDiff = scaleDiff;
        this.lifeLength = lifeLength;
        this.particleTextureSelector = particleTextureSelector;
    }

    @Override
    public String getWorldId() {
        return worldId;
    }

    @Override
    public Vector3 getLocation() {
        return location;
    }

    @Override
    public float getRotation() {
        return rotation;
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public TextureRegion getTexture() {
        return particleTextureSelector.getTextureRegion(this, elapsedTime, lifeLength);
    }

    @Override
    public boolean updateParticle(float gravity, float timeSinceLastUpdateInSeconds) {
        velocity.add(0, gravityInfluence * gravity * timeSinceLastUpdateInSeconds, 0);
        location.add(
                velocity.x * timeSinceLastUpdateInSeconds,
                velocity.y * timeSinceLastUpdateInSeconds,
                velocity.z * timeSinceLastUpdateInSeconds);
        rotation += rotationVelocity * timeSinceLastUpdateInSeconds;
        scale += scaleDiff * timeSinceLastUpdateInSeconds;

        elapsedTime += timeSinceLastUpdateInSeconds;

        return elapsedTime < lifeLength;
    }
}
