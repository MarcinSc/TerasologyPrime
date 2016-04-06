package com.gempukku.terasology.landd.component;

import com.gempukku.secsy.entity.Component;

public interface TargetingComponent extends Component {
    float getTranslateFromLocationX();

    float getTranslateFromLocationY();

    float getTranslateFromLocationZ();
}
