package com.gempukku.secsy.entity.index;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityListener;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.InternalEntityManager;
import com.gempukku.secsy.entity.SimpleEntity;

import java.util.HashSet;
import java.util.Set;

@RegisterSystem(
        shared = EntityIndexManager.class)
public class SimpleEntityIndexManager implements EntityIndexManager, EntityRefCreationCallback, EntityListener,
        LifeCycleSystem {
    @In
    private InternalEntityManager internalEntityManager;

    private Set<ComponentEntityIndex> indices = new HashSet<>();

    @Override
    public void initialize() {
        internalEntityManager.addEntityListener(this);
    }

    @Override
    public EntityIndex addIndexOnComponents(Class<? extends Component>... components) {
        ComponentEntityIndex index = new ComponentEntityIndex(this, components);
        indices.add(index);
        return index;
    }

    @Override
    public EntityRef createEntityRef(SimpleEntity entity) {
        return internalEntityManager.wrapEntity(entity);
    }

    @Override
    public void entitiesModified(Iterable<SimpleEntity> entity) {
        for (ComponentEntityIndex index : indices) {
            index.entitiesModified(entity);
        }
    }
}
