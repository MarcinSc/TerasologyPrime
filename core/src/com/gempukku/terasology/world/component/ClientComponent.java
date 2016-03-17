package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;

public interface ClientComponent extends Component {
    String getClientId();
    void setClientId(String clientId);

    int getChunkDistanceX();
    void setChunkDistanceX(int chunkDistance);

    int getChunkDistanceY();
    void setChunkDistanceY(int chunkDistance);

    int getChunkDistanceZ();
    void setChunkDistanceZ(int chunkDistance);
}
