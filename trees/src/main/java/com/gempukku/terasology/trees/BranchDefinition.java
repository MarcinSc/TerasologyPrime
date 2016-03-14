package com.gempukku.terasology.trees;

import java.util.LinkedList;
import java.util.List;

public class BranchDefinition {
    public float horizontalLeavesScale;
    public float verticalLeavesScale;
    public final float rotationY;
    public final float rotationZ;
    public final List<BranchSegmentDefinition> segments = new LinkedList<>();

    public BranchDefinition(float rotationY, float rotationZ, float horizontalLeavesScale, float verticalLeavesScale) {
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.horizontalLeavesScale = horizontalLeavesScale;
        this.verticalLeavesScale = verticalLeavesScale;
    }
}
