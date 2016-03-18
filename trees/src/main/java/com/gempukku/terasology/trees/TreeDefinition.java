package com.gempukku.terasology.trees;

public class TreeDefinition {
    public final String leavesShape;
    public final BranchDefinition trunkDefinition;
    public final String barkTexture;
    public final String leavesTexture;

    public TreeDefinition(String leavesShape,
                          String barkTexture, String leavesTexture,
                          BranchDefinition trunkDefinition) {
        this.leavesShape = leavesShape;
        this.barkTexture = barkTexture;
        this.leavesTexture = leavesTexture;
        this.trunkDefinition = trunkDefinition;
    }
}
