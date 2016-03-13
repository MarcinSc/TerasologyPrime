package com.gempukku.secsy.network.server;

import com.gempukku.secsy.entity.EntityRef;

public interface ClientManager {
    void addClient(String clientId, EntityRef clientEntity, ClientCommunication clientCommunication);

    void removeClient(String clientId);

    void addClientEntityRelevanceRule(ClientEntityRelevanceRule clientEntityRelevanceRule);

    void removeClientEntityRelevanceRule(ClientEntityRelevanceRule clientEntityRelevanceRule);

    void addEntityComponentFieldFilter(EntityComponentFieldFilter entityComponentFieldFilter);

    void removeEntityComponentFieldFilter(EntityComponentFieldFilter entityComponentFieldFilter);
}
