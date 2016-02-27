package com.gempukku.secsy.entity;

public interface InternalEntityManager {
    void addEntityEventListener(EntityEventListener entityEventListener);

    void removeEntityEventListener(EntityEventListener entityEventListener);
}
