package com.gempukku.terasology.particle.impl.selector.color;

import com.badlogic.gdx.graphics.Color;
import com.gempukku.terasology.particle.impl.SimpleParticle;

public class FadeParticleColorSelector implements ParticleColorSelector {
    private Color baseColor;

    public FadeParticleColorSelector(Color baseColor) {
        this.baseColor = baseColor;
    }

    @Override
    public Color getColor(SimpleParticle particle) {
        float multiplier = 1 - (particle.elapsedTime / particle.lifeLength);

        return new Color(baseColor.r, baseColor.g, baseColor.b, baseColor.a * multiplier);
    }
}
