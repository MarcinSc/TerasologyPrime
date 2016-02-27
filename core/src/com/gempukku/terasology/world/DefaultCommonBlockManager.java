package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.prefab.PrefabData;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.component.CommonBlockComponent;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        shared = CommonBlockManager.class)
public class DefaultCommonBlockManager implements CommonBlockManager, LifeCycleSystem {
    @In
    private PrefabManager prefabManager;

    private Map<String, PrefabData> commonBlockPrefabsById;

    @Override
    public synchronized PrefabData getCommonBlockById(String id) {
        if (commonBlockPrefabsById == null)
            populateMap();

        return commonBlockPrefabsById.get(id);
    }

    private void populateMap() {
        commonBlockPrefabsById = new HashMap<>();

        for (PrefabData prefabData : prefabManager.findPrefabsWithComponents(CommonBlockComponent.class)) {
            String blockId = (String) prefabData.getComponents().get(CommonBlockComponent.class.getSimpleName()).getFields().get("id");
            commonBlockPrefabsById.put(blockId, prefabData);
        }
    }
}
