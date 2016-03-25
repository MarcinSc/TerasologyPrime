package com.gempukku.terasology.trees.leaves;

import com.badlogic.gdx.graphics.Texture;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.trees.LSystemTreeBlockGeometryGenerator;
import com.gempukku.terasology.world.chunk.geometry.BlockGeometryGenerator;

public interface LeavesGenerator {
    LSystemTreeBlockGeometryGenerator.LSystemCallback createLeavesCallback(
            EntityRef entityRef, BlockGeometryGenerator.VertexOutput vertexOutput, Texture texture);
}
