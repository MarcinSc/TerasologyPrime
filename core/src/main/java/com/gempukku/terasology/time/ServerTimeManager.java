package com.gempukku.terasology.time;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.world.MultiverseManager;
import com.gempukku.terasology.world.component.MultiverseComponent;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = {TimeManager.class, InternalTimeManager.class})
public class ServerTimeManager implements TimeManager, InternalTimeManager {
    @In
    private MultiverseManager multiverseManager;

    private long timeSinceLastUpdate = 0;

    @Override
    public void updateMultiverseTime(long timeDiff) {
        EntityRef multiverseEntity = multiverseManager.getMultiverseEntity();
        MultiverseComponent multiverse = multiverseEntity.getComponent(MultiverseComponent.class);
        long lastTime = multiverse.getTime();
        timeSinceLastUpdate = timeDiff;
        multiverse.setTime(lastTime + timeDiff);
        multiverseEntity.saveComponents(multiverse);
    }

    @Override
    public long getMultiverseTime() {
        EntityRef multiverseEntity = multiverseManager.getMultiverseEntity();
        MultiverseComponent multiverse = multiverseEntity.getComponent(MultiverseComponent.class);
        return multiverse.getTime();
    }

    @Override
    public long getTimeSinceLastUpdate() {
        return timeSinceLastUpdate;
    }
}
