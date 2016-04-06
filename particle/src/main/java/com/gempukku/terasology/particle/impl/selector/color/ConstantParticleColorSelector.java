package com.gempukku.terasology.particle.impl.selector.color;

import com.badlogic.gdx.graphics.Color;
import com.gempukku.terasology.particle.impl.SimpleParticle;

public class ConstantParticleColorSelector implements ParticleColorSelector {
    private Color color;

    public ConstantParticleColorSelector(Color color) {
        this.color = color;
    }

    @Override
    public Color getColor(SimpleParticle particle) {
        return color;
    }
}
