package com.gempukku.terasology.movement;

public interface MovementController {
    void updateMovement(float yawDiff, float speed, float verticalSpeed);

    float getYaw();

    float getHorizontalSpeed();

    float getVerticalSpeed();

    float getMaximumSpeed();

    float getJumpSpeed();
}
