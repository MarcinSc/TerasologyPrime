package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.PlayerManager;
import com.gempukku.terasology.world.component.ClientComponent;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = PlayerManager.class)
public class DefaultPlayerManager implements PlayerManager {
    @In
    private EntityManager entityManager;

    @Override
    public EntityRef createPlayer(String id) {
        EntityRef entity = entityManager.createEntity();
        ClientComponent player = entity.createComponent(ClientComponent.class);
        player.setClientId(id);
        player.setChunkDistanceX(3);
        player.setChunkDistanceY(3);
        player.setChunkDistanceZ(3);
        entity.saveComponents(player);
        return entity;
    }

    @Override
    public EntityRef getPlayer(String id) {
        for (EntityRef entityRef : entityManager.getEntitiesWithComponents(ClientComponent.class)) {
            if (entityRef.getComponent(ClientComponent.class).getClientId().equals(id))
                return entityRef;
        }
        return null;
    }
}
