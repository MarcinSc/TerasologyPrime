package com.gempukku.terasology.trees;

import com.badlogic.gdx.graphics.Texture;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.graphics.environment.BlockMeshGenerator;

public interface LeavesGenerator {
    LSystemTreeBlockMeshGenerator.LSystemCallback createLeavesCallback(
            EntityRef entityRef, BlockMeshGenerator.VertexOutput vertexOutput, Texture texture);
}
