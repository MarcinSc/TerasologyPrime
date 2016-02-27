package com.gempukku.secsy.entity.io;

import com.gempukku.secsy.entity.Component;

import java.util.Collection;

public interface ComponentData {
    Class<? extends Component> getComponentClass();
    Iterable<FieldNameAndValue> getFields();

    class FieldNameAndValue {
        public final String name;
        public final Object value;

        public FieldNameAndValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}
