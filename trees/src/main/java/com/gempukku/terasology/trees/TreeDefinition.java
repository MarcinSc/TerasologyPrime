package com.gempukku.terasology.trees;

import java.util.LinkedList;
import java.util.List;

public class TreeDefinition {
    public final float trunkRotationY;
    public final List<TrunkSegmentDefinition> segments = new LinkedList<>();

    public TreeDefinition(float trunkRotationY) {
        this.trunkRotationY = trunkRotationY;
    }

    public void addSegment(TrunkSegmentDefinition segment) {
        segments.add(segment);
    }
}
