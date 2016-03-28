package com.gempukku.terasology.physics.basic;

import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.terasology.physics.component.BasicCylinderPhysicsObjectComponent;
import com.gempukku.terasology.world.event.AfterPlayerCreatedEvent;

@RegisterSystem(
        profiles = {"basicPhysics", NetProfiles.AUTHORITY})
public class AuthorityBasicPhysicsEngine {
    @ReceiveEvent
    public void afterPlayerCreated(AfterPlayerCreatedEvent event, EntityRef entity) {
        BasicCylinderPhysicsObjectComponent playerPhysicalProperties = entity.createComponent(BasicCylinderPhysicsObjectComponent.class);
        playerPhysicalProperties.setRadius(0.4f);
        playerPhysicalProperties.setHeight(1.9f);
        entity.saveComponents(playerPhysicalProperties);
    }
}
