package com.gempukku.terasology.landd.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface FiresMissileParticleComponent extends Component {
    String getMissileTexture();

    int getRowCount();

    int getColumnCount();

    float getColorR();

    float getColorG();

    float getColorB();

    float getRotation();

    float getRotationVelocity();

    float getScale();

    float getScaleDiff();

    float getParticleLifeLength();

    float getGravityInfluence();
}
