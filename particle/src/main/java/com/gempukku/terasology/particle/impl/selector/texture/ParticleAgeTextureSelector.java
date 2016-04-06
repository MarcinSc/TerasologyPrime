package com.gempukku.terasology.particle.impl.selector.texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.particle.impl.SimpleParticle;
import com.gempukku.terasology.procedural.FastMath;

public class ParticleAgeTextureSelector implements ParticleTextureSelector {
    private TextureRegion[] textures;

    public ParticleAgeTextureSelector(TextureRegion... textures) {
        this.textures = textures;
    }

    @Override
    public TextureRegion getTextureRegion(SimpleParticle particle) {
        float textureTime = particle.lifeLength / textures.length;
        int index = FastMath.floor(particle.elapsedTime / textureTime);
        if (index >= textures.length)
            index = textures.length - 1;
        return textures[index];
    }
}
