package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface MultiverseComponent extends Component {
    void setTime(long time);

    long getTime();
}
