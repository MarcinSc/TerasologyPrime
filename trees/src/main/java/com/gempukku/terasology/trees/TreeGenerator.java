package com.gempukku.terasology.trees;

import com.gempukku.secsy.entity.EntityRef;

public interface TreeGenerator {
    TreeDefinition generateTreeDefinition(EntityRef entityRef);
}
