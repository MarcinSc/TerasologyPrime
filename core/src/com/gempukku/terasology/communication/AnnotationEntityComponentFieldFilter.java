package com.gempukku.terasology.communication;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.network.server.ClientManager;
import com.gempukku.secsy.entity.network.server.EntityComponentFieldFilter;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class AnnotationEntityComponentFieldFilter implements EntityComponentFieldFilter, LifeCycleSystem {
    @In
    private ClientManager clientManager;

    @Override
    public void initialize() {
        clientManager.addEntityComponentFieldFilter(this);
    }

    @Override
    public boolean isComponentFieldRelevant(EntityRef clientEntity, EntityRef entity, Class<? extends Component> component, String field) {
        SharedComponent sharedComponent = component.getAnnotation(SharedComponent.class);
        if (sharedComponent != null) {
            for (String notSharedField : sharedComponent.notSharedFields()) {
                if (notSharedField.equals(field)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isComponentRelevant(EntityRef clientEntity, EntityRef entity, Class<? extends Component> component) {
        return component.isAnnotationPresent(SharedComponent.class);
    }
}
