package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;

import java.util.Map;

public interface ShapeAndTextureComponent extends Component {
    void setShapeId(String id);
    String getShapeId();

    void setParts(Map<String, String> parts);
    Map<String, String> getParts();

    void setOpaque(boolean opaque);
    boolean isOpaque();
}
