package com.gempukku.terasology.landd.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.landd.component.FiresMissileParticleComponent;
import com.gempukku.terasology.landd.event.FireMissileEvent;
import com.gempukku.terasology.particle.ParticleEmitter;
import com.gempukku.terasology.particle.impl.SimpleParticle;
import com.gempukku.terasology.particle.impl.selector.color.FadeParticleColorSelector;
import com.gempukku.terasology.particle.impl.selector.texture.ParticleAgeCombinedTextureSelector;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.time.TimeManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
    @In
    private PrefabManager prefabManager;

    private Map<FireMissileEvent, FiresMissileParticleComponent> missileEvents = new HashMap<>();

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);

        Set<String> particleTextures = new HashSet<>();
        for (EntityData entityData : prefabManager.findPrefabsWithComponents(FiresMissileParticleComponent.class)) {
            particleTextures.add((String) entityData.getComponent(FiresMissileParticleComponent.class).getFields().get("missileTexture"));
        }

        textureAtlasRegistry.registerTextures(ParticleEmitter.PARTICLES_ATLAS_NAME, particleTextures);
    }

    @Override
    public void update() {
        long multiverseTime = timeManager.getMultiverseTime();
        Iterator<Map.Entry<FireMissileEvent, FiresMissileParticleComponent>> iterator = missileEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<FireMissileEvent, FiresMissileParticleComponent> entry = iterator.next();
            FireMissileEvent event = entry.getKey();
            FiresMissileParticleComponent missile = entry.getValue();
            if (event.fireTime + event.duration * 1000f < multiverseTime)
                iterator.remove();
            else {
                float diff = (multiverseTime - event.fireTime) / 1000f / event.duration;
                Vector3 location = new Vector3(
                        event.startX + diff * (event.endX - event.startX),
                        event.startY + diff * (event.endY - event.startY),
                        event.startZ + diff * (event.endZ - event.startZ));
                particleEmitter.emitParticle(
                        new SimpleParticle(event.worldId, location, new Vector3(), missile.getGravityInfluence(),
                                missile.getRotation(), missile.getRotationVelocity(), missile.getScale(), missile.getScaleDiff(),
                                missile.getParticleLifeLength(),
                                new ParticleAgeCombinedTextureSelector(
                                        textureAtlasProvider.getTexture(ParticleEmitter.PARTICLES_ATLAS_NAME, missile.getMissileTexture()), missile.getRowCount(), missile.getColumnCount()),
                                new FadeParticleColorSelector(
                                        new Color(missile.getColorR(), missile.getColorG(), missile.getColorB(), 1f))));
            }
        }
    }

    @ReceiveEvent
    public void missileFired(FireMissileEvent event, EntityRef firingEntity, FiresMissileParticleComponent firesMissile) {
        missileEvents.put(event, firesMissile);
    }
}
