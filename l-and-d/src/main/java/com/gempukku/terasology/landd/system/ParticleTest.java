package com.gempukku.terasology.landd.system;

import com.badlogic.gdx.graphics.Color;
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
import com.gempukku.terasology.particle.impl.selector.color.ConstantParticleColorSelector;
import com.gempukku.terasology.particle.impl.selector.texture.ParticleAgeCombinedTextureSelector;
import com.gempukku.terasology.particle.impl.selector.texture.ParticleTextureSelector;
import com.gempukku.terasology.time.TimeManager;

import java.util.Arrays;
import java.util.Random;

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

    private Random rnd = new Random(0);
    private float timeSinceLastEmit;

    private ParticleTextureSelector explosionTexture;

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

        timeSinceLastEmit += timeManager.getTimeSinceLastUpdate() / 1000f;
        if (timeSinceLastEmit >= 0.01) {
            timeSinceLastEmit = 0;
            float angle = rnd.nextFloat() * 2 * (float) Math.PI;
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);
            particleEmitter.emitParticle(
                    new SimpleParticle("world", new Vector3(5, 2, 5), new Vector3(0.1f * x, 2, 0.1f * z), 0.05f, 0, 0, 1, 0, 10,
                            explosionTexture, new ConstantParticleColorSelector(Color.GREEN)));
        }
    }
}
