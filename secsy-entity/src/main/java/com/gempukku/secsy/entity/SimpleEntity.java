package com.gempukku.secsy.entity;

import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleEntity implements EntityData {
    private InternalComponentManager internalComponentManager;
    public int id;
    public Map<Class<? extends Component>, Component> entityValues = new HashMap<>();
    public boolean exists = true;

    public SimpleEntity(InternalComponentManager internalComponentManager, int id) {
        this.internalComponentManager = internalComponentManager;
        this.id = id;
    }

    @Override
    public int getEntityId() {
        return id;
    }

    @Override
    public Iterable<ComponentData> getComponents() {
        return entityValues.entrySet().stream().map(
                componentEntry -> new ComponentData() {
                    @Override
                    public Class<? extends Component> getComponentClass() {
                        return componentEntry.getKey();
                    }

                    @Override
                    public Map<String, Object> getFields() {
                        Map<String, Object> result = new HashMap<String, Object>();
                        internalComponentManager.getComponentFieldTypes(componentEntry.getValue()).entrySet().stream().forEach(
                                fieldDef -> {
                                    String fieldName = fieldDef.getKey();
                                    Object fieldValue = internalComponentManager.getComponentFieldValue(componentEntry.getValue(), fieldName, componentEntry.getKey());
                                    result.put(fieldName, fieldValue);
                                });
                        return result;
                    }
                }).collect(Collectors.toList());
    }
}
