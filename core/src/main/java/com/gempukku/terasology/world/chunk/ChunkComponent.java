package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface ChunkComponent extends Component {
    String getWorldId();

    int getX();

    int getY();

    int getZ();
}
