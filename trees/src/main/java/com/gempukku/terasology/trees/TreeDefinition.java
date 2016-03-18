package com.gempukku.terasology.trees;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.terasology.graphics.shape.ShapeDef;

public class TreeDefinition {
    public final ShapeDef leavesShape;
    public final BranchDefinition trunkDefinition;
    public final TextureRegion barkTexture;
    public final TextureRegion leavesTexture;

    public TreeDefinition(ShapeDef leavesShape,
                          TextureRegion barkTexture, TextureRegion leavesTexture,
                          BranchDefinition trunkDefinition) {
        this.leavesShape = leavesShape;
        this.barkTexture = barkTexture;
        this.leavesTexture = leavesTexture;
        this.trunkDefinition = trunkDefinition;
    }
}
