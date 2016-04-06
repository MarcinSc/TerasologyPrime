package com.gempukku.terasology.particle;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;

public interface Particle {
    boolean updateParticle(float gravity, float timeSinceLastUpdateInSeconds);

    String getWorldId();

    Vector3 getLocation();

    float getRotation();

    float getScale();

    TextureRegion getTexture();
}
