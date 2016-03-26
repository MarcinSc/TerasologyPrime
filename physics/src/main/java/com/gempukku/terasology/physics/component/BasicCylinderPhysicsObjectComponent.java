package com.gempukku.terasology.physics.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface BasicCylinderPhysicsObjectComponent extends Component {
    float getRadius();

    void setRadius(float radius);

    float getHeight();

    void setHeight(float height);
}
