package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface ChunkComponent extends Component {
    @GetProperty("chunkBlocks")
    String[] getChunkBlocks();

    @SetProperty("chunkBlocks")
    void setChunkBlocks(String[] chunkBlocks);

    @GetProperty("worldId")
    String getWorldId();

    @SetProperty("worldId")
    void setWorldId(String worldId);

    @GetProperty("x")
    int getX();

    @SetProperty("x")
    void setX(int x);

    @GetProperty("y")
    int getY();

    @SetProperty("y")
    void setY(int y);

    @GetProperty("z")
    int getZ();

    @SetProperty("z")
    void setZ(int z);
}
