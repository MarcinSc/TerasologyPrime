package com.gempukku.terasology.particle.selector;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.Particle;
import com.gempukku.terasology.procedural.FastMath;

public class ParticleAgeTextureSelector implements ParticleTextureSelector {
    private TextureRegion[] textures;

    public ParticleAgeTextureSelector(TextureRegion... textures) {
        this.textures = textures;
    }

    @Override
    public TextureRegion getTextureRegion(Particle particle, float elapsedTime, float lifeLength) {
        float textureTime = lifeLength / textures.length;
        return textures[FastMath.floor(elapsedTime / textureTime)];
    }
}
