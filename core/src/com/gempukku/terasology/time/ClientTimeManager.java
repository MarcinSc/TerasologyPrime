package com.gempukku.terasology.time;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.game.InternalGameLoopListener;
import com.gempukku.terasology.world.component.MultiverseComponent;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = TimeManager.class)
public class ClientTimeManager implements TimeManager, InternalGameLoopListener, LifeCycleSystem {
    @In
    private InternalGameLoop gameLoop;
    @In
    private EntityManager entityManager;

    private long lastUpdateTime = -1;
    private long timeSinceLastUpdate = 0;

    @Override
    public void initialize() {
        gameLoop.addInternalGameLoopListener(this);
    }

    @Override
    public void preUpdate() {
        if (lastUpdateTime != -1) {
            long currentTime = System.currentTimeMillis();
            timeSinceLastUpdate = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
        } else {
            long currentTime = System.currentTimeMillis();
            lastUpdateTime = currentTime;
            timeSinceLastUpdate = 0;
        }
    }

    @Override
    public void postUpdate() {

    }

    @Override
    public long getMultiverseTime() {
        return getMultiverseEntity().getComponent(MultiverseComponent.class).getTime();
    }

    @Override
    public long getTimeSinceLastUpdate() {
        return timeSinceLastUpdate;
    }

    private EntityRef getMultiverseEntity() {
        for (EntityRef entityRef : entityManager.getEntitiesWithComponents(MultiverseComponent.class)) {
            return entityRef;
        }
        return null;
    }
}
