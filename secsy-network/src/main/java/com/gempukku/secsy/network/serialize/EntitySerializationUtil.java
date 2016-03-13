package com.gempukku.secsy.network.serialize;

import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.network.server.EntityComponentFieldFilter;

import java.util.Map;

public class EntitySerializationUtil {
    private EntitySerializationUtil() {
    }

    public static EntityInformation serializeEntity(InternalComponentManager componentManager, EntityRef clientEntity,
                                                    int entityId, EntityRef entity, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) {
        EntityInformation entityInformation = new EntityInformation();
        entityInformation.setEntityId(entityId);
        for (Class<? extends Component> componentClass : entity.listComponents()) {
            if (filtersAcceptComponent(clientEntity, entity, componentClass, componentFieldFilters)) {
                Component component = entity.getComponent(componentClass);

                ComponentInformation componentInformation = new ComponentInformation(componentClass);

                Map<String, Class<?>> fieldTypes = componentManager.getComponentFieldTypes(component);
                for (Map.Entry<String, Class<?>> fieldTypeEntry : fieldTypes.entrySet()) {
                    String fieldName = fieldTypeEntry.getKey();
                    if (filtersAcceptField(clientEntity, entity, componentClass, fieldName, componentFieldFilters)) {
                        componentInformation.addField(fieldName, componentManager.getComponentFieldValue(component, fieldName, fieldTypeEntry.getValue()));
                    }
                }
                entityInformation.addComponent(componentInformation);
            }
        }
        return entityInformation;
    }

    private static boolean filtersAcceptField(EntityRef clientEntity, EntityRef entity, Class<? extends Component> componentClass, String fieldName, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) {
        for (EntityComponentFieldFilter componentFieldFilter : componentFieldFilters) {
            if (componentFieldFilter.isComponentFieldRelevant(clientEntity, entity, componentClass, fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean filtersAcceptComponent(EntityRef clientEntity, EntityRef entity, Class<? extends Component> componentClass, Iterable<? extends EntityComponentFieldFilter> componentFieldFilters) {
        for (EntityComponentFieldFilter componentFieldFilter : componentFieldFilters) {
            if (componentFieldFilter.isComponentRelevant(clientEntity, entity, componentClass)) {
                return true;
            }
        }
        return false;
    }
}
