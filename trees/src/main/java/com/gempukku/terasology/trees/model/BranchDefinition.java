package com.gempukku.terasology.trees.model;

import java.util.LinkedList;
import java.util.List;

public class BranchDefinition {
    public final float rotationY;
    public final float rotationZ;
    public final List<BranchSegmentDefinition> segments = new LinkedList<>();

    public BranchDefinition(float rotationY, float rotationZ) {
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }
}
