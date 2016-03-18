package com.gempukku.terasology.world.blockEntity;

import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.terasology.world.component.BlockComponent;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RegisterSystem(
        shared = BlockEntityManager.class)
public class ThreadSafeBlockEntityManager implements BlockEntityManager {
    private Map<String, EntityRef> entities = new ConcurrentHashMap<>();

    @ReceiveEvent
    public void blockEntityAdded(AfterComponentAdded event, EntityRef entity, LocationComponent location, BlockComponent block) {
        entities.put(getKey(location.getWorldId(), Math.round(location.getX()), Math.round(location.getY()), Math.round(location.getZ())), entity);
    }

    @ReceiveEvent
    public void blockEntityRemoved(BeforeComponentRemoved event, EntityRef entity, LocationComponent location, BlockComponent block) {
        entities.remove(getKey(location.getWorldId(), Math.round(location.getX()), Math.round(location.getY()), Math.round(location.getZ())));
    }

    @Override
    public EntityRef getBlockEntityAt(String worldId, int x, int y, int z) {
        return entities.get(getKey(worldId, x, y, z));
    }

    private String getKey(String worldId, int x, int y, int z) {
        return worldId + "|" + x + "," + y + "," + z;
    }
}
