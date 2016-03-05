package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;

public interface ClientComponent extends Component {
    @GetProperty("clientId")
    String getClientId();

    @SetProperty("clientId")
    void setClientId(String clientId);

    @GetProperty("chunkDistanceX")
    int getChunkDistanceX();

    @SetProperty("chunkDistanceX")
    void setChunkDistanceX(int chunkDistance);

    @GetProperty("chunkDistanceY")
    int getChunkDistanceY();

    @SetProperty("chunkDistanceY")
    void setChunkDistanceY(int chunkDistance);

    @GetProperty("chunkDistanceZ")
    int getChunkDistanceZ();

    @SetProperty("chunkDistanceZ")
    void setChunkDistanceZ(int chunkDistance);
}
