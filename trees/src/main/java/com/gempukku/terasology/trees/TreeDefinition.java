package com.gempukku.terasology.trees;

public class TreeDefinition {
    public final String leavesShape;
    public final BranchDefinition trunkDefinition;

    public TreeDefinition(String leavesShape, BranchDefinition trunkDefinition) {
        this.leavesShape = leavesShape;
        this.trunkDefinition = trunkDefinition;
    }
}
