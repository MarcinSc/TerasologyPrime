package com.gempukku.secsy.entity.relevance;

import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.context.annotation.API;
import com.gempukku.secsy.entity.io.EntityData;

import java.util.Iterator;

/**
 * Interface for classes that want to participate in determining which entities are relevant at which times,
 * and are responsible for storing irrelevant and restoring any newly relevant entities.
 */
@API
public interface EntityRelevanceRule {
    /**
     * Called once on each cleanup run to update the relevance rule to establish its internal rules governing
     * relevance of entities.
     */
    void determineRelevance();

    /**
     * Called to determine if the specific entity is governed by this rule (responsible for storing it) and
     * if so, if this entity is no longer relevant (should be unloaded).
     * @param entityRef
     * @return
     */
    boolean isEntityRuledByRuleAndIrrelevant(EntityRef entityRef);

    /**
     * Called to store the specified irrelevant entities governed by this entity.
     * @param entities
     */
    void storeEntities(Iterable<? extends EntityData> entities);

    /**
     *
     * @return
     */
    Iterable<? extends EntityData> getNewRelevantEntities();

    /**
     * Called after all the produced new relevant entitites have been loaded.
     */
    void newRelevantEntitiesLoaded();
}
