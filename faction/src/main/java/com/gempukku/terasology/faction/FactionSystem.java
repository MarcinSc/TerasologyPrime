package com.gempukku.terasology.faction;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.terasology.utils.tree.DimensionalMap;
import com.gempukku.terasology.utils.tree.SpaceTree;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY, shared = FactionManager.class)
public class FactionSystem implements FactionManager, LifeCycleSystem {
    @In
    private EntityIndexManager entityIndexManager;

    private SpaceTree<EntityRef> factionMembers = new SpaceTree<EntityRef>(3);
    private EntityIndex factionsIndex;

    @Override
    public void initialize() {
        factionsIndex = entityIndexManager.addIndexOnComponents(FactionComponent.class);
    }

    @ReceiveEvent
    public void factionMemberAdded(AfterComponentAdded componentAdded, EntityRef member, FactionMemberComponent factionMemberComponent, LocationComponent location) {
        factionMembers.add(new float[]{location.getX(), location.getY(), location.getZ()}, member);
    }

    @ReceiveEvent
    public void factionMemberMoved(AfterComponentUpdated componentUpdated, EntityRef member, LocationComponent location) {
        if (member.hasComponent(FactionMemberComponent.class)) {
            LocationComponent oldLocation = componentUpdated.getOldComponent(LocationComponent.class);
            LocationComponent newLocation = componentUpdated.getNewComponent(LocationComponent.class);
            factionMembers.remove(new float[]{oldLocation.getX(), oldLocation.getY(), oldLocation.getZ()});
            factionMembers.add(new float[]{newLocation.getX(), newLocation.getY(), newLocation.getZ()}, member);
        }
    }

    @ReceiveEvent
    public void factionMemberRemoved(BeforeComponentRemoved componentRemoved, EntityRef member, FactionComponent faction, LocationComponent location) {
        factionMembers.remove(
                new float[]{location.getX(), location.getY(), location.getZ()});
    }

    @Override
    public Iterable<EntityRef> findClosestEnemies(String faction, Vector3 position, float distance) {
        Set<String> enemyFactions = getEnemyFactions(faction);
        if (enemyFactions == null)
            return Collections.emptySet();

        List<EntityRef> result = new LinkedList<>();
        for (DimensionalMap.Entry<EntityRef> entityRefEntry : factionMembers.findNearest(new float[]{position.x, position.y, position.z}, 100, distance)) {
            EntityRef factionMember = entityRefEntry.value;
            FactionMemberComponent factionComp = factionMember.getComponent(FactionMemberComponent.class);
            if (enemyFactions.contains(factionComp.getFactionId()))
                result.add(factionMember);
        }

        return result;
    }

    private Set<String> getEnemyFactions(String factionId) {
        for (EntityRef entityRef : factionsIndex.getEntities()) {
            FactionComponent faction = entityRef.getComponent(FactionComponent.class);
            if (faction.getFactionId().equals(factionId)) {
                return faction.getOpposingFactions();
            }
        }
        return null;
    }
}
