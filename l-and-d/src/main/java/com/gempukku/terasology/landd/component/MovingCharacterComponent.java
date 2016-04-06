package com.gempukku.terasology.landd.component;

import com.gempukku.secsy.entity.Component;

public interface MovingCharacterComponent extends Component {
    float getSpeedX();

    float getSpeedY();

    float getSpeedZ();
}
