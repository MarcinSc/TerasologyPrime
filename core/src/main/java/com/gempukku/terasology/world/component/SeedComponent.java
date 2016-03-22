package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface SeedComponent extends Component {
    long getSeed();

    void setSeed(long seed);
}
