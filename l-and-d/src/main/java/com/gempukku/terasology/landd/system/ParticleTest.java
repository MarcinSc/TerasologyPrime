package com.gempukku.terasology.landd.system;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.particle.ParticleEmitter;
import com.gempukku.terasology.particle.impl.SimpleParticle;
import com.gempukku.terasology.particle.selector.ParticleSingleTextureSelector;
import com.gempukku.terasology.time.TimeManager;

import java.util.Arrays;

@RegisterSystem(profiles = NetProfiles.CLIENT)
public class ParticleTest implements GameLoopListener, LifeCycleSystem {
    @In
    private GameLoop gameLoop;
    @In
    private ParticleEmitter particleEmitter;
    @In
    private TimeManager timeManager;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;

    private float timeSinceLastEmit;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
        textureAtlasRegistry.registerTextures("particles", Arrays.asList("blockTiles/trees/PineBark.png"));
    }

    @Override
    public void update() {
        timeSinceLastEmit += timeManager.getTimeSinceLastUpdate() / 1000f;
        if (timeSinceLastEmit >= 1) {
            timeSinceLastEmit = 0;
            particleEmitter.emitParticle(
                    new SimpleParticle(new Vector3(2, 2, 2), new Vector3(1, 0, 1), 0, 0, 0, 1, 0, 10,
                            new ParticleSingleTextureSelector(textureAtlasProvider.getTexture("particles", "blockTiles/trees/PineBark.png"))));
        }
    }
}
