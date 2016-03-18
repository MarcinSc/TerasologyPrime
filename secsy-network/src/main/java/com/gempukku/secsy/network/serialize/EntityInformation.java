package com.gempukku.secsy.network.serialize;

import com.gempukku.secsy.entity.Component;
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

    @Override
    public ComponentData getComponent(Class<? extends Component> componentClass) {
        for (ComponentInformation component : components) {
            if (component.getComponentClass() == componentClass)
                return component;
        }
        return null;
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
