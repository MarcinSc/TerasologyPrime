package com.gempukku.terasology.physics.basic;

import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.terasology.movement.MovementComponent;
import com.gempukku.terasology.movement.MovementRequestEvent;
import com.gempukku.terasology.physics.component.BasicCylinderPhysicsObjectComponent;
import com.gempukku.terasology.world.component.LocationComponent;
import com.gempukku.terasology.world.event.AfterPlayerCreatedEvent;

@RegisterSystem(
        profiles = {"basicPhysics", NetProfiles.AUTHORITY})
public class AuthorityBasicPhysicsEngine {
    @ReceiveEvent
    public void movementRequestProcess(MovementRequestEvent event, EntityRef client, LocationComponent location, MovementComponent movement) {
        movement.setSpeed(event.horizontalSpeed);
        movement.setVerticalSpeed(event.verticalSpeed);
        movement.setYaw(event.yaw);

        location.setX(event.positionX);
        location.setY(event.positionY);
        location.setZ(event.positionZ);

        client.saveChanges();
    }

    @ReceiveEvent
    public void afterPlayerCreated(AfterPlayerCreatedEvent event, EntityRef entity) {
        BasicCylinderPhysicsObjectComponent playerPhysicalProperties = entity.createComponent(BasicCylinderPhysicsObjectComponent.class);
        playerPhysicalProperties.setRadius(0.4f);
        playerPhysicalProperties.setHeight(1.9f);
        entity.saveChanges();
    }
}
