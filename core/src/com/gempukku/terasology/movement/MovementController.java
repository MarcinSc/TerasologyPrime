package com.gempukku.terasology.movement;

public interface MovementController {
    void updateMovement(float yaw, float pitch, float speed, float verticalSpeed);

    float getYaw();

    float getPitch();

    float getHorizontalSpeed();

    float getVerticalSpeed();

    float getMaximumSpeed();

    float getJumpSpeed();
}
