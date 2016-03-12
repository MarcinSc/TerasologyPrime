package com.gempukku.terasology.graphics.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;

public interface GeneratedBlockMeshComponent extends Component {
    @GetProperty("generatorType")
    String getGeneratorType();

    @SetProperty("generatorType")
    void setGeneratorType(String generatorType);
}
