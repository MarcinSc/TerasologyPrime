package com.gempukku.terasology.landd.component;

import com.gempukku.secsy.entity.Component;

public interface RangedAttackCharacterComponent extends Component {
    float getFiringRange();

    long getFiringCooldown();

    void setLastFired(long time);

    long getLastFired();
}
