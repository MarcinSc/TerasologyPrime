package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;

public interface ClientComponent extends Component {
    String getClientId();
    void setClientId(String clientId);

    int getChunkHorizontalDistance();

    void setChunkHorizontalDistance(int chunkDistance);

    int getChunkVerticalDistance();

    void setChunkVerticalDistance(int chunkDistance);
}
