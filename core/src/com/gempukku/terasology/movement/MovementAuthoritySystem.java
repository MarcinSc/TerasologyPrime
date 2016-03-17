package com.gempukku.terasology.movement;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.time.TimeManager;
import com.gempukku.terasology.world.component.LocationComponent;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class MovementAuthoritySystem implements LifeCycleSystem, GameLoopListener {
    @In
    private EntityManager entityManager;
    @In
    private GameLoop gameLoop;
    @In
    private TimeManager timeManager;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
    }

    @Override
    public void update() {
        float timeSinceLastUpdateInSeconds = timeManager.getTimeSinceLastUpdate() / 1000f;

        for (EntityRef movingEntity : entityManager.getEntitiesWithComponents(MovementComponent.class, LocationComponent.class)) {
            MovementComponent movement = movingEntity.getComponent(MovementComponent.class);
            LocationComponent location = movingEntity.getComponent(LocationComponent.class);

            float v = movement.getVerticalSpeed() * timeSinceLastUpdateInSeconds;
            location.setY(location.getY() + v);

            float speed = movement.getSpeed();
            float timeHorizontalComponent = speed * timeSinceLastUpdateInSeconds;
            float x = timeHorizontalComponent * (float) Math.cos(movement.getYaw());
            float z = timeHorizontalComponent * (float) Math.sin(movement.getYaw());
            location.setX(location.getX() + x);
            location.setZ(location.getZ() + z);

            movingEntity.saveComponents(location);
        }
    }

    @ReceiveEvent
    public void updateMovementRequest(MovementRequestEvent event, EntityRef entity, MovementComponent movement, LocationComponent location) {
        movement.setSpeed(event.horizontalSpeed);
        movement.setVerticalSpeed(event.verticalSpeed);
        movement.setYaw(event.yaw);

        location.setX(event.positionX);
        location.setY(event.positionY);
        location.setZ(event.positionZ);

        if (entity.hasComponent(CameraComponent.class)) {
            CameraComponent camera = entity.getComponent(CameraComponent.class);
            Vector3 direction = new Vector3(0, 0, 1);
            direction.rotateRad(new Vector3(0, 1, 0), event.yaw);
            camera.setDirectionX(direction.x);
            camera.setDirectionY(direction.y);
            camera.setDirectionZ(direction.z);
            entity.saveComponents(camera);
        }

        entity.saveComponents(movement, location);
    }
}
