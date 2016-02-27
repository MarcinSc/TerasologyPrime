package com.gempukku.secsy.entity.io;

import java.util.Collection;

public interface EntityData {
    int getEntityId();
    Iterable<ComponentData> getComponents();
}
