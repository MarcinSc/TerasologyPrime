package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.io.ComponentData;

import java.util.HashSet;
import java.util.Set;

public class DefaultComponentData implements ComponentData {
    private final Class<? extends Component> clazz;
    private Set<FieldNameAndValue> fields = new HashSet<>();

    public DefaultComponentData(Class<? extends Component> clazz) {
        this.clazz = clazz;
    }

    public DefaultComponentData(ComponentData toCopy) {
        this(toCopy.getComponentClass());

        for (FieldNameAndValue fieldNameAndValue : toCopy.getFields()) {
            addField(fieldNameAndValue.name, fieldNameAndValue.value);
        }
    }

    public void addField(String fieldName, Object fieldValue) {
        fields.add(new FieldNameAndValue(fieldName, fieldValue));
    }

    @Override
    public Class<? extends Component> getComponentClass() {
        return clazz;
    }

    @Override
    public Iterable<FieldNameAndValue> getFields() {
        return fields;
    }
}
