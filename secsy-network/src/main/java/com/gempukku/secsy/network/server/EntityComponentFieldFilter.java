package com.gempukku.secsy.network.server;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityRef;

public interface EntityComponentFieldFilter {
    boolean isComponentRelevant(EntityRef clientEntity, EntityRef entity, Class<? extends Component> component);

    boolean isComponentFieldRelevant(EntityRef clientEntity, EntityRef entity, Class<? extends Component> component, String field);
}
