package com.gempukku.terasology.graphics.shape;

import java.util.List;

public class ShapeDef {
    private List<ShapePartDef> shapeParts;
    private List<String> fullParts;

    public List<ShapePartDef> getShapeParts() {
        return shapeParts;
    }

    public void setShapeParts(List<ShapePartDef> shapeParts) {
        this.shapeParts = shapeParts;
    }

    public List<String> getFullParts() {
        return fullParts;
    }

    public void setFullParts(List<String> fullParts) {
        this.fullParts = fullParts;
    }
}
