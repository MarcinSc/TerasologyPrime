package com.gempukku.terasology.trees.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface LeavesDefinitionComponent extends Component {
    String getLeavesShape();

    String getLeavesTexture();
}
