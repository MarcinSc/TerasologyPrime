package com.gempukku.terasology.trees;

import java.util.LinkedList;
import java.util.List;

public class TreeSegmentDefinition {
    public final float rotateX;
    public final float rotateZ;
    public final List<TreeDefinition> branches = new LinkedList<>();
    public float length;
    public float radius;

    public TreeSegmentDefinition(float length, float radius, float rotateX, float rotateZ) {
        this.length = length;
        this.radius = radius;
        this.rotateX = rotateX;
        this.rotateZ = rotateZ;
    }
}
