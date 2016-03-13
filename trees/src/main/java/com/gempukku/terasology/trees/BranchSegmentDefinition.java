package com.gempukku.terasology.trees;

public class BranchSegmentDefinition {
    public final float rotateZ;
    public float length;
    public float radius;

    public BranchSegmentDefinition(float length, float radius, float rotateZ) {
        this.length = length;
        this.radius = radius;
        this.rotateZ = rotateZ;
    }
}
