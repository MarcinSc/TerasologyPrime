package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface ChunkComponent extends Component {
    String getWorldId();
    void setWorldId(String worldId);

    int getX();
    void setX(int x);

    int getY();
    void setY(int y);

    int getZ();
    void setZ(int z);
}
