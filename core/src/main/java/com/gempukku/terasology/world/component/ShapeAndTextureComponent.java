package com.gempukku.terasology.world.component;

import com.gempukku.secsy.entity.Component;

import java.util.Map;

public interface ShapeAndTextureComponent extends Component {
    String getShapeId();

    Map<String, String> getParts();

    boolean isOpaque();
}
