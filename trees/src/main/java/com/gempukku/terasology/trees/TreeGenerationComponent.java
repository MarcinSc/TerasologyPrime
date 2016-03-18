package com.gempukku.terasology.trees;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface TreeGenerationComponent extends Component {
    String getGenerationType();

    void setGenerationType(String generationType);
}
