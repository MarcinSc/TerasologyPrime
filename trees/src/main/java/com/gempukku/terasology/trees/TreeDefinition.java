package com.gempukku.terasology.trees;

import java.util.LinkedList;
import java.util.List;

public class TreeDefinition {
    public final float rotationY;
    public final float rotationZ;
    public final List<TreeSegmentDefinition> segments = new LinkedList<>();

    public TreeDefinition(float rotationY, float rotationZ) {
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }
}
