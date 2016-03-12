package com.gempukku.terasology.physics.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface PhysicsObjectComponent extends Component {
    @GetProperty("shape")
    String getShape();

    @SetProperty("shape")
    void setShape(String shape);

    @GetProperty("parameters")
    String getParameters();

    @SetProperty("parameters")
    void setParameters(String parameters);

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

    @GetProperty("mass")
    float getMass();

    @SetProperty("mass")
    void setMass(float mass);

}
