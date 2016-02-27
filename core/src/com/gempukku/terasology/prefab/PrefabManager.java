package com.gempukku.terasology.prefab;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.io.EntityData;

public interface PrefabManager {
    Iterable<PrefabData> findPrefabsWithComponents(Class<? extends Component>... components);

    PrefabData getPrefabByName(String name);

    EntityData convertToEntityData(PrefabData prefabData);
}
