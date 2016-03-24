package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent(notSharedFields = "blockIds")
public interface ChunkComponent extends Component {
    String getWorldId();

    int getX();

    int getY();

    int getZ();

    short[] getBlockIds();
}
