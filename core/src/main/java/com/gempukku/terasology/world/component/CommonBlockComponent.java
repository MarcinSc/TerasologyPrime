package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;

public interface CommonBlockComponent extends Component {
    void setId(String id);
    String getId();
}
