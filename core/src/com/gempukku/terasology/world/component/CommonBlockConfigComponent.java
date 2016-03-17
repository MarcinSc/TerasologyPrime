package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface CommonBlockConfigComponent extends Component {
    void setCommonBlocks(String[] commonBlockIds);
    String[] getCommonBlocks();
}
