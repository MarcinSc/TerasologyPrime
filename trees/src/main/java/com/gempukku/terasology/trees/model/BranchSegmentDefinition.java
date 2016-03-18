package com.gempukku.terasology.trees.model;

import java.util.LinkedList;
import java.util.List;

public class BranchSegmentDefinition {
    public float horizontalLeavesScale;
    public float verticalLeavesScale;
    public final float rotateX;
    public final float rotateZ;
    public final List<BranchDefinition> branches = new LinkedList<>();
    public float length;
    public float radius;

    public BranchSegmentDefinition(float length, float radius, float rotateX, float rotateZ) {
        this.length = length;
        this.radius = radius;
        this.rotateX = rotateX;
        this.rotateZ = rotateZ;
    }
}
