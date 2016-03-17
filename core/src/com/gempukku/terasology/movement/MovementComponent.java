package com.gempukku.terasology.movement;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface MovementComponent extends Component {
    float getMaxSpeed();

    void setMaxSpeed(float maxSpeed);

    float getJumpSpeed();

    void setJumpSpeed(float jumpSpeed);

    void setSpeed(float speed);

    float getSpeed();

    void setVerticalSpeed(float verticalSpeed);

    float getVerticalSpeed();

    float getYaw();

    void setYaw(float yaw);
}
