package com.gempukku.terasology.trees;

import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.trees.model.TreeDefinition;

public interface TreeGenerator {
    TreeDefinition generateTreeDefinition(EntityRef entityRef);
}
