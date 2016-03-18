package com.gempukku.terasology.world;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterEntityLoaded;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.component.CommonBlockComponent;
import com.gempukku.terasology.world.component.CommonBlockConfigComponent;
import com.gempukku.terasology.world.component.MultiverseComponent;
import com.gempukku.terasology.world.component.ShapeAndTextureComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RegisterSystem(
        shared = CommonBlockManager.class)
public class DefaultCommonBlockManager implements CommonBlockManager, LifeCycleSystem {
    @In
    private PrefabManager prefabManager;
    @In(optional = true)
    private TextureAtlasRegistry textureAtlasRegistry;

    private volatile Map<String, EntityData> commonBlockPrefabsById;
    private volatile String[] commonBlockIds;

    @Override
    public void initialize() {
        if (textureAtlasRegistry != null) {
            Set<String> texturePaths = new HashSet<>();

            for (EntityData prefabData : prefabManager.findPrefabsWithComponents(CommonBlockComponent.class, ShapeAndTextureComponent.class)) {
                for (String partTexture : ((Map<String, String>) prefabData.getComponent(ShapeAndTextureComponent.class).getFields().get("parts")).values()) {
                    texturePaths.add(partTexture);
                }
            }
            textureAtlasRegistry.registerTextures(texturePaths);
        }
    }

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

    private EntityData getCommonBlockById(String id) {
        init();

        if (id == null)
            throw new IllegalArgumentException("Can't ask for block with null id");

        return commonBlockPrefabsById.get(id);
    }

    @Override
    public EntityData getCommonBlockById(short id) {
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

        for (EntityData prefabData : prefabManager.findPrefabsWithComponents(CommonBlockComponent.class)) {
            String blockId = (String) prefabData.getComponent(CommonBlockComponent.class).getFields().get("id");
            commonBlockPrefabsById.put(blockId, prefabData);
        }
    }
}
