package com.gempukku.terasology.component;

import com.gempukku.secsy.entity.Component;

public interface TerasologyComponentManager {
    Class<? extends Component> getComponentByName(String name);

    String getNameByComponent(Class<? extends Component> componentClass);
}
