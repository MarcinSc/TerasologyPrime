package com.gempukku.terasology.trees.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface SimpleTreeDefinitionComponent extends Component {
    String getBarkTexture();

    String getLeavesTexture();

    String getLeavesShape();

    int getMaxGenerations();
}
