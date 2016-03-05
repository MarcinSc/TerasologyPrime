package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface WorldComponent extends Component {
    @SetProperty("worldId")
    void setWorldId(String worldId);

    @GetProperty("worldId")
    String getWorldId();
}
