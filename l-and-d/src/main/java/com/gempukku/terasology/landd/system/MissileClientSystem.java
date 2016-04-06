package com.gempukku.terasology.landd.system;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.landd.event.FireMissileEvent;
import com.gempukku.terasology.particle.ParticleEmitter;
import com.gempukku.terasology.particle.impl.SimpleParticle;
import com.gempukku.terasology.particle.selector.ParticleAgeCombinedTextureSelector;
import com.gempukku.terasology.particle.selector.ParticleTextureSelector;
import com.gempukku.terasology.time.TimeManager;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = NetProfiles.CLIENT)
public class MissileClientSystem implements GameLoopListener, LifeCycleSystem {
    @In
    private ParticleEmitter particleEmitter;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private GameLoop gameLoop;
    @In
    private TimeManager timeManager;

    private ParticleTextureSelector explosionTexture;

    private List<FireMissileEvent> missileEvents = new LinkedList<>();

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
        textureAtlasRegistry.registerTextures("particles", Arrays.asList("particle/explosion.png"));
    }

    @Override
    public void update() {
        if (explosionTexture == null) {
            explosionTexture = new ParticleAgeCombinedTextureSelector(
                    textureAtlasProvider.getTexture("particles", "particle/explosion.png"), 5, 5);
        }

        long multiverseTime = timeManager.getMultiverseTime();
        Iterator<FireMissileEvent> iterator = missileEvents.iterator();
        while (iterator.hasNext()) {
            FireMissileEvent event = iterator.next();
            if (event.fireTime + event.duration * 1000f < multiverseTime)
                iterator.remove();
            else {
                float diff = (multiverseTime - event.fireTime) / 1000f / event.duration;
                Vector3 location = new Vector3(
                        event.startX + diff * (event.endX - event.startX),
                        event.startY + diff * (event.endY - event.startY),
                        event.startZ + diff * (event.endZ - event.startZ));
                particleEmitter.emitParticle(
                        new SimpleParticle(event.worldId, location, new Vector3(), 0f, 0f, 0f, 2f, 0f, 1f, explosionTexture));
            }
        }
    }

    @ReceiveEvent
    public void missileFired(FireMissileEvent event, EntityRef firingEntity) {
        missileEvents.add(event);
    }
}
