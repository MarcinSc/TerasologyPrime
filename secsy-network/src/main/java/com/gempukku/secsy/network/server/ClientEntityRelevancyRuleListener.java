package com.gempukku.secsy.network.server;

import com.gempukku.secsy.entity.EntityRef;

public interface ClientEntityRelevancyRuleListener {
    void entityRelevancyChanged(String clientId, Iterable<EntityRef> entities);
}
