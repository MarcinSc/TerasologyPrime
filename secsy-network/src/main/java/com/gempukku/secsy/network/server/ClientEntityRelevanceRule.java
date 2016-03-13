package com.gempukku.secsy.network.server;

import com.gempukku.secsy.entity.EntityRef;

public interface ClientEntityRelevanceRule {
    boolean isEntityRelevantForClient(EntityRef clientEntity, EntityRef entity);

    void addClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener);

    void removeClientEntityRelevancyRuleListener(ClientEntityRelevancyRuleListener listener);
}
