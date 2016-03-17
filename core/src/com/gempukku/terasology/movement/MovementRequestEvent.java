package com.gempukku.terasology.movement;

import com.gempukku.secsy.entity.event.Event;
import com.gempukku.secsy.network.ToServerEvent;

@ToServerEvent
public class MovementRequestEvent extends Event {
    public final float positionX;
    public final float positionY;
    public final float positionZ;

    public final float verticalSpeed;
    public final float yaw;
    public final float horizontalSpeed;

    public MovementRequestEvent(float positionX, float positionY, float positionZ, float verticalSpeed, float horizontalSpeed, float yaw) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.verticalSpeed = verticalSpeed;
        this.horizontalSpeed = horizontalSpeed;
        this.yaw = yaw;
    }
}
