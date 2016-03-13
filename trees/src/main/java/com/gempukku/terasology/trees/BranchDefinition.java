package com.gempukku.terasology.trees;

import java.util.LinkedList;
import java.util.List;

public class BranchDefinition {
    public final float angleY;
    public final float angleZ;
    public final List<BranchSegmentDefinition> branchSegments = new LinkedList<>();

    public BranchDefinition(float angleY, float angleZ) {
        this.angleY = angleY;
        this.angleZ = angleZ;
    }
}
