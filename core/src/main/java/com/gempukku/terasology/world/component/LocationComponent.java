package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface LocationComponent extends Component {
    void setWorldId(String worldId);
    String getWorldId();

    void setX(float x);
    float getX();

    void setY(float y);
    float getY();

    void setZ(float z);
    float getZ();
}
