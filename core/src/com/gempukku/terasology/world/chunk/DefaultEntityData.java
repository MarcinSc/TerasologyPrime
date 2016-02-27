package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;

import java.util.HashSet;
import java.util.Set;

public class DefaultEntityData implements EntityData {
    private final int entityId;
    private final Set<ComponentData> components = new HashSet<>();

    public DefaultEntityData(int entityId) {
        this.entityId = entityId;
    }

    public DefaultEntityData(EntityData toCopy) {
        this(toCopy.getEntityId());

        for (ComponentData componentData : toCopy.getComponents()) {
            addComponent(new DefaultComponentData(componentData));
        }
    }

    public void addComponent(ComponentData componentData) {
        components.add(componentData);
    }

    @Override
    public Iterable<ComponentData> getComponents() {
        return components;
    }

    @Override
    public int getEntityId() {
        return entityId;
    }
}
