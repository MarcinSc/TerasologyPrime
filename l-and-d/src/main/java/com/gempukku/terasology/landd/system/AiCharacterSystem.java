package com.gempukku.terasology.landd.system;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.terasology.faction.FactionManager;
import com.gempukku.terasology.faction.FactionMemberComponent;
import com.gempukku.terasology.landd.component.AiCharacterComponent;
import com.gempukku.terasology.landd.component.MovingCharacterComponent;
import com.gempukku.terasology.landd.component.RangedAttackCharacterComponent;
import com.gempukku.terasology.landd.event.FireMissileEvent;
import com.gempukku.terasology.time.TimeManager;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Iterator;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class AiCharacterSystem implements GameLoopListener, LifeCycleSystem {
    @In
    private GameLoop gameLoop;
    @In
    private EntityIndexManager entityIndexManager;
    @In
    private FactionManager factionManager;
    @In
    private TimeManager timeManager;

    private EntityIndex aiEntitiesIndex;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
        aiEntitiesIndex = entityIndexManager.addIndexOnComponents(AiCharacterComponent.class);
    }

    @Override
    public void update() {
        for (EntityRef entityRef : aiEntitiesIndex.getEntities()) {
            processEntity(entityRef);
        }
    }

    private void processEntity(EntityRef entityRef) {
        long multiverseTime = timeManager.getMultiverseTime();

        RangedAttackCharacterComponent rangedAttack = entityRef.getComponent(RangedAttackCharacterComponent.class);
        FactionMemberComponent factionMember = entityRef.getComponent(FactionMemberComponent.class);
        LocationComponent location = entityRef.getComponent(LocationComponent.class);
        if (rangedAttack != null && factionMember != null && location != null) {
            Iterable<EntityRef> closestEnemies = factionManager.findClosestEnemies(factionMember.getFactionId(),
                    new Vector3(location.getX(), location.getY(), location.getZ()), rangedAttack.getFiringRange());
            EntityRef closestEnemy = getFirst(closestEnemies);
            if (closestEnemy != null) {
                if (rangedAttack.getLastFired() + rangedAttack.getFiringCooldown() < multiverseTime) {
                    LocationComponent targetLocation = closestEnemy.getComponent(LocationComponent.class);

                    rangedAttack.setLastFired(multiverseTime);
                    entityRef.saveComponents(rangedAttack);

                    float speed = 1f;
                    Vector3 start = new Vector3(location.getX(), location.getY(), location.getZ());
                    Vector3 destination = new Vector3(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ());

                    entityRef.send(
                            new FireMissileEvent(
                                    location.getWorldId(), start.x, start.y, start.z,
                                    destination.x, destination.y, destination.z, start.dst(destination) / speed));
                }
                return;
            }
        }
        MovingCharacterComponent movingCharacter = entityRef.getComponent(MovingCharacterComponent.class);
        if (movingCharacter != null && location != null) {
            float timeSinceLastUpdateInSeconds = timeManager.getTimeSinceLastUpdate() / 1000f;
            location.setX(location.getX() + movingCharacter.getSpeedX() * timeSinceLastUpdateInSeconds);
            location.setY(location.getY() + movingCharacter.getSpeedY() * timeSinceLastUpdateInSeconds);
            location.setZ(location.getZ() + movingCharacter.getSpeedZ() * timeSinceLastUpdateInSeconds);
            entityRef.saveComponents(location);
            return;
        }
    }

    private EntityRef getFirst(Iterable<EntityRef> closestEnemies) {
        Iterator<EntityRef> iterator = closestEnemies.iterator();
        if (iterator.hasNext())
            return iterator.next();
        else
            return null;
    }
}
