package com.gempukku.terasology.prefab;

import java.util.Collections;
import java.util.Map;

public class PrefabComponentData {
    private final Map<String, Object> fields;

    public PrefabComponentData(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(fields);
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}
