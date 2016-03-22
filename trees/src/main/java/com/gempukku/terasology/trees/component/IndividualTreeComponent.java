package com.gempukku.terasology.trees.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface IndividualTreeComponent extends Component {
    int getGeneration();

    void setGeneration(int generation);
}
