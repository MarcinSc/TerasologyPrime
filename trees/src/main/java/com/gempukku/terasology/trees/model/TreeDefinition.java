package com.gempukku.terasology.trees.model;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TreeDefinition {
    public final BranchDefinition trunkDefinition;
    public final TextureRegion barkTexture;
    public final String leavesGenerator;

    public TreeDefinition(BranchDefinition trunkDefinition, TextureRegion barkTexture, String leavesGenerator) {
        this.trunkDefinition = trunkDefinition;
        this.barkTexture = barkTexture;
        this.leavesGenerator = leavesGenerator;
    }
}
