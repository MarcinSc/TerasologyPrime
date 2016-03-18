package com.gempukku.terasology.graphics.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface CameraComponent extends Component {
    void setNear(float near);
    float getNear();

    void setFar(float far);
    float getFar();

    void setDirectionX(float x);
    float getDirectionX();

    void setDirectionY(float y);
    float getDirectionY();

    void setDirectionZ(float z);
    float getDirectionZ();

    void setTranslateFromLocationX(float x);
    float getTranslateFromLocationX();

    void setTranslateFromLocationY(float y);
    float getTranslateFromLocationY();

    void setTranslateFromLocationZ(float z);
    float getTranslateFromLocationZ();

    void setActive(boolean active);
    boolean isActive();
}
