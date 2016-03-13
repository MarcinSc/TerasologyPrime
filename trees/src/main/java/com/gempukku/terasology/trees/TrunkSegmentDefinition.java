package com.gempukku.terasology.trees;

import java.util.List;

public class TrunkSegmentDefinition {
    public final float rotateX;
    public final float rotateZ;
    public float length;
    public float radius;
    public final List<BranchDefinition> branches;

    public TrunkSegmentDefinition(List<BranchDefinition> branches, float length, float radius, float rotateX, float rotateZ) {
        this.branches = branches;
        this.length = length;
        this.radius = radius;
        this.rotateX = rotateX;
        this.rotateZ = rotateZ;
    }
}
