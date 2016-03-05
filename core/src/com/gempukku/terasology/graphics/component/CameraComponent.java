package com.gempukku.terasology.graphics.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface CameraComponent extends Component {
    @SetProperty("near")
    void setNear(float near);

    @GetProperty("near")
    float getNear();

    @SetProperty("far")
    void setFar(float far);

    @GetProperty("far")
    float getFar();

    @SetProperty("directionX")
    void setDirectionX(float x);
    @GetProperty("directionX")
    float getDirectionX();

    @SetProperty("directionY")
    void setDirectionY(float y);
    @GetProperty("directionY")
    float getDirectionY();

    @SetProperty("directionZ")
    void setDirectionZ(float z);
    @GetProperty("directionZ")
    float getDirectionZ();

    @SetProperty("translateFromLocationX")
    void setTranslateFromLocationX(float x);
    @GetProperty("translateFromLocationX")
    float getTranslateFromLocationX();

    @SetProperty("translateFromLocationY")
    void setTranslateFromLocationY(float y);
    @GetProperty("translateFromLocationY")
    float getTranslateFromLocationY();

    @SetProperty("translateFromLocationZ")
    void setTranslateFromLocationZ(float z);
    @GetProperty("translateFromLocationZ")
    float getTranslateFromLocationZ();

    @SetProperty("active")
    void setActive(boolean active);

    @GetProperty("active")
    boolean isActive();
}
