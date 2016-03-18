package com.gempukku.secsy.entity.io;

import com.gempukku.secsy.entity.Component;

public interface EntityData {
    int getEntityId();

    Iterable<? extends ComponentData> getComponents();

    ComponentData getComponent(Class<? extends Component> componentClass);
}
