package com.gempukku.secsy.entity.io;

public interface EntityData {
    int getEntityId();

    Iterable<? extends ComponentData> getComponents();
}
