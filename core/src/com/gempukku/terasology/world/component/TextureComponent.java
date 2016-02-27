package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.component.map.GetProperty;
import com.gempukku.secsy.entity.component.map.SetProperty;

import java.util.Map;

public interface TextureComponent extends Component {
    @SetProperty("parts")
    void setParts(Map<String, String> parts);

    @GetProperty("parts")
    Map<String, String> getParts();

    @SetProperty("opaque")
    void setOpaque(boolean opaque);

    @GetProperty("opaque")
    boolean isOpaque();
}
