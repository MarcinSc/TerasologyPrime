package com.gempukku.terasology.prefab;

import java.util.Collections;
import java.util.Map;

public class PrefabData {
    private final Map<String, PrefabComponentData> components;

    public PrefabData(Map<String, PrefabComponentData> components) {
        this.components = Collections.unmodifiableMap(components);
    }

    public Map<String, PrefabComponentData> getComponents() {
        return components;
    }
}
