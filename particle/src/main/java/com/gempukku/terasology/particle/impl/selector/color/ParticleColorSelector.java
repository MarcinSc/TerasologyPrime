package com.gempukku.terasology.particle.impl.selector.color;

import com.badlogic.gdx.graphics.Color;
import com.gempukku.terasology.particle.impl.SimpleParticle;

public interface ParticleColorSelector {
    Color getColor(SimpleParticle particle);
}
