package com.gempukku.terasology.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface LocationComponent extends Component {
    @SetProperty("worldId")
    void setWorldId(String worldId);

    @GetProperty("worldId")
    String getWorldId();

    @SetProperty("x")
    void setX(float x);

    @GetProperty("x")
    float getX();

    @SetProperty("y")
    void setY(float y);

    @GetProperty("y")
    float getY();

    @SetProperty("z")
    void setZ(float z);

    @GetProperty("z")
    float getZ();
}
