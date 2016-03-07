package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterEntityLoaded;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.prefab.PrefabData;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.component.CommonBlockComponent;
import com.gempukku.terasology.world.component.CommonBlockConfigComponent;
import com.gempukku.terasology.world.component.MultiverseComponent;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        shared = CommonBlockManager.class)
public class DefaultCommonBlockManager implements CommonBlockManager, LifeCycleSystem {
    @In
    private PrefabManager prefabManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;

    private volatile Map<String, PrefabData> commonBlockPrefabsById;
    private volatile String[] commonBlockIds;

    @ReceiveEvent
    public void multiverseCreated(AfterComponentAdded event, EntityRef entity, MultiverseComponent multiverse) {
        init();

        if (!entity.hasComponent(CommonBlockConfigComponent.class)) {
            this.commonBlockIds = new String[commonBlockPrefabsById.size()];
            int index = 0;
            for (String commonBlockId : commonBlockPrefabsById.keySet()) {
                commonBlockIds[index++] = commonBlockId;
            }

            CommonBlockConfigComponent component = entity.createComponent(CommonBlockConfigComponent.class);
            component.setCommonBlocks(commonBlockIds);
            entity.saveComponents(component);
        } else {
            commonBlockIds = entity.getComponent(CommonBlockConfigComponent.class).getCommonBlocks();
        }
    }

    @ReceiveEvent
    public void multiverseLoaded(AfterEntityLoaded event, EntityRef entity, MultiverseComponent multiverseComponent, CommonBlockConfigComponent config) {
        commonBlockIds = config.getCommonBlocks();
    }

    private PrefabData getCommonBlockById(String id) {
        init();

        if (id == null)
            throw new IllegalArgumentException("Can't ask for block with null id");

        return commonBlockPrefabsById.get(id);
    }

    @Override
    public PrefabData getCommonBlockById(short id) {
        return getCommonBlockById(commonBlockIds[id]);
    }

    @Override
    public int getCommonBlockCount() {
        return commonBlockIds.length;
    }

    @Override
    public short getCommonBlockId(String commonBlockId) {
        init();
        for (short i = 0; i < commonBlockIds.length; i++) {
            if (commonBlockIds[i].equals(commonBlockId))
                return i;
        }
        return -1;
    }

    private void init() {
        if (commonBlockPrefabsById == null)
            populateMap();
    }

    private void populateMap() {
        commonBlockPrefabsById = new HashMap<>();

        for (PrefabData prefabData : prefabManager.findPrefabsWithComponents(CommonBlockComponent.class)) {
            String simpleName = terasologyComponentManager.getNameByComponent(CommonBlockComponent.class);
            String blockId = (String) prefabData.getComponents().get(simpleName).getFields().get("id");
            commonBlockPrefabsById.put(blockId, prefabData);
        }
    }
}
