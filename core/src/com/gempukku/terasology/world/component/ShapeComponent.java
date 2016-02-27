package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;

public interface ShapeComponent extends Component {
    @SetProperty("id")
    void setId(String id);

    @GetProperty("id")
    String getId();
}
