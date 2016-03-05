package com.gempukku.secsy.entity.network.serialize;

import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;

import java.util.LinkedList;
import java.util.List;

public class EntityInformation implements EntityData {
    private int entityId;
    private List<ComponentInformation> components = new LinkedList<>();

    public EntityInformation() {
    }

    public EntityInformation(EntityData toCopy) {
        setEntityId(toCopy.getEntityId());

        for (ComponentData componentData : toCopy.getComponents()) {
            addComponent(new ComponentInformation(componentData));
        }
    }


    public Iterable<ComponentInformation> getComponents() {
        return components;
    }

    public void addComponent(ComponentInformation componentInformation) {
        components.add(componentInformation);
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }
}
