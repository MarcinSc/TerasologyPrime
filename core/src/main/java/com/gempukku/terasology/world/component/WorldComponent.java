package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface WorldComponent extends Component {
    void setWorldId(String worldId);
    String getWorldId();

    int getDayLength();

    int getDayStartDifferenceFromMultiverse();
}
