package com.gempukku.terasology.movement;

public interface MovementController {
    enum Mode {
        WALKING, FREE_FALL;
    }

    void updateMovement(Mode mode, float yaw, float pitch, float speed, float verticalSpeed);

    float getYaw();

    float getPitch();

    float getHorizontalSpeed();

    float getVerticalSpeed();

    float getMaximumSpeed();

    float getJumpSpeed();

    Mode getMode();
}
