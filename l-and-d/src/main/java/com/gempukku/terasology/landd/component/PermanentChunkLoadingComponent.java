package com.gempukku.terasology.landd.component;

import com.gempukku.secsy.entity.Component;

public interface PermanentChunkLoadingComponent extends Component {
    String getWorldId();

    int getMinimumChunkX();

    int getMinimumChunkY();

    int getMinimumChunkZ();

    int getMaximumChunkX();

    int getMaximumChunkY();

    int getMaximumChunkZ();
}
