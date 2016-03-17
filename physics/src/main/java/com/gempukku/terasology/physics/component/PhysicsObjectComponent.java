package com.gempukku.terasology.physics.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface PhysicsObjectComponent extends Component {
    String getShape();
    void setShape(String shape);

    String getParameters();
    void setParameters(String parameters);

    void setTranslateFromLocationX(float x);
    float getTranslateFromLocationX();

    void setTranslateFromLocationY(float y);
    float getTranslateFromLocationY();

    void setTranslateFromLocationZ(float z);
    float getTranslateFromLocationZ();

    float getMass();
    void setMass(float mass);
}
