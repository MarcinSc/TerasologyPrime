package com.gempukku.terasology.movement;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.secsy.network.client.ServerEventBus;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.LocationComponent;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = MovementController.class)
public class MovementClientSystem implements MovementController, GameLoopListener, LifeCycleSystem {
    @In
    private EntityManager entityManager;
    @In
    private GameLoop gameLoop;
    @In
    private ServerEventBus serverEventBus;

    private float maxSpeed;
    private float jumpSpeed;

    private float positionX;
    private float positionY;
    private float positionZ;

    private float yaw;
    private float horizontalSpeed;
    private float verticalSpeed;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
    }

    @Override
    public void updateMovement(float yaw, float speed, float verticalSpeed) {
        this.yaw = yaw;
        this.horizontalSpeed = speed;
        this.verticalSpeed = verticalSpeed;

        serverEventBus.sendEventToServer(new MovementRequestEvent(positionX, positionY, positionZ, verticalSpeed, speed, yaw));
    }

    @ReceiveEvent
    public void movementSet(AfterComponentAdded event, EntityRef entity, MovementComponent movement, ClientComponent client) {
        maxSpeed = movement.getMaxSpeed();
        jumpSpeed = movement.getJumpSpeed();
    }

    @ReceiveEvent
    public void movementUpdated(AfterComponentUpdated event, EntityRef entity, MovementComponent movement, ClientComponent client) {
        maxSpeed = movement.getMaxSpeed();
        jumpSpeed = movement.getJumpSpeed();
    }

    @Override
    public float getHorizontalSpeed() {
        return horizontalSpeed;
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    @Override
    public float getVerticalSpeed() {
        return verticalSpeed;
    }

    @Override
    public float getJumpSpeed() {
        return jumpSpeed;
    }

    @Override
    public float getMaximumSpeed() {
        return maxSpeed;
    }

    @Override
    public void update(long delta) {
        // TODO for now just assume this
        float timeSinceLastUpdateInSeconds = 0.1f;

        for (EntityRef movingEntity : entityManager.getEntitiesWithComponents(MovementComponent.class, LocationComponent.class)) {
            if (!movingEntity.hasComponent(ClientComponent.class)) {
                // Simulate movement based on what server says
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
            } else {
                // Simulate movement based on our own values
                LocationComponent location = movingEntity.getComponent(LocationComponent.class);
                CameraComponent camera = movingEntity.getComponent(CameraComponent.class);

                float v = verticalSpeed * timeSinceLastUpdateInSeconds;
                location.setY(positionY + v);

                float speed = horizontalSpeed;
                float timeHorizontalComponent = speed * timeSinceLastUpdateInSeconds;
                float x = timeHorizontalComponent * (float) Math.cos(yaw);
                float z = timeHorizontalComponent * (float) Math.sin(yaw);
                location.setX(positionX + x);
                location.setZ(positionZ + z);

                positionX = location.getX();
                positionY = location.getY();
                positionZ = location.getZ();

                camera.setDirectionX((float) Math.cos(yaw));
                camera.setDirectionY(0);
                camera.setDirectionZ((float) Math.sin(yaw));

                movingEntity.saveComponents(location, camera);
                serverEventBus.sendEventToServer(new MovementRequestEvent(positionX, positionY, positionZ, verticalSpeed, horizontalSpeed, yaw));
            }
        }
    }
}
