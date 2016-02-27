package com.gempukku.terasology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.gempukku.secsy.context.SECSyContext;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.graphics.RenderingEngine;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.world.MultiverseManager;
import com.gempukku.terasology.world.component.PlayerComponent;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.Set;

public class TerasologyApplication extends ApplicationAdapter {
    private SECSyContext systemContext;

    private InternalGameLoop internalGameLoop;
    private RenderingEngine renderingEngine;

    private FPSLogger fpsLogger = new FPSLogger();
    private long startTime;

    @Override
    public void create() {
        Set<String> profiles = new HashSet<>();
        // This is server context (for now we have everything on server context)
        profiles.add(NetProfiles.AUTHORITY);
        profiles.add(NetProfiles.CLIENT);
        // We use components created in map
        profiles.add("mapComponent");
        // World generator
        profiles.add("hillsWorld");

        Configuration scanBasedOnAnnotations = new ConfigurationBuilder()
                .setScanners(new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forJavaClassPath());

        systemContext = new SECSyContext(profiles, new Reflections(scanBasedOnAnnotations));
        systemContext.startup();

        internalGameLoop = systemContext.getSystem(InternalGameLoop.class);
        renderingEngine = systemContext.getSystem(RenderingEngine.class);

        MultiverseManager multiverseManager = systemContext.getSystem(MultiverseManager.class);
        multiverseManager.createWorld("world");

        PlayerManager playerManager = systemContext.getSystem(PlayerManager.class);
        EntityRef player = playerManager.createPlayer("playerId");

        LocationComponent playerLocation = player.createComponent(LocationComponent.class);
        playerLocation.setX(0);
        playerLocation.setY(0);
        playerLocation.setZ(0);
        playerLocation.setWorldId("world");

        CameraComponent playerCamera = player.createComponent(CameraComponent.class);
        playerCamera.setNear(0.1f);
        playerCamera.setFar(100f);
        playerCamera.setActive(true);
        playerCamera.setDirectionX(0);
        playerCamera.setDirectionY(0);
        playerCamera.setDirectionZ(-1);
        playerCamera.setTranslateFromLocationX(0f);
        playerCamera.setTranslateFromLocationY(1.8f);
        playerCamera.setTranslateFromLocationZ(0f);

        player.saveComponents(playerLocation, playerCamera);

        startTime = System.currentTimeMillis();
    }

    @Override
    public void render() {
        EntityRef playerEntity = systemContext.getSystem(PlayerManager.class).getPlayer("playerId");
        CameraComponent camera = playerEntity.getComponent(CameraComponent.class);
        LocationComponent location = playerEntity.getComponent(LocationComponent.class);

        float rotateStep = 0.05f;
        float stepLength = 0.5f;

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            location.setX(location.getX()+camera.getDirectionX()*stepLength);
            location.setY(location.getY()+camera.getDirectionY()*stepLength);
            location.setZ(location.getZ()+camera.getDirectionZ()*stepLength);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            location.setX(location.getX()-camera.getDirectionX()*stepLength);
            location.setY(location.getY()-camera.getDirectionY()*stepLength);
            location.setZ(location.getZ()-camera.getDirectionZ()*stepLength);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            location.setY(location.getY()+stepLength);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            location.setY(location.getY()-stepLength);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            location.setX(location.getX()-camera.getDirectionX()*stepLength);
            location.setY(location.getY()-camera.getDirectionY()*stepLength);
            location.setZ(location.getZ()-camera.getDirectionZ()*stepLength);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            Vector3 direction = new Vector3(camera.getDirectionX(), camera.getDirectionY(), camera.getDirectionZ());
            direction.rotateRad(new Vector3(0, 1, 0), rotateStep);
            camera.setDirectionX(direction.x);
            camera.setDirectionY(direction.y);
            camera.setDirectionZ(direction.z);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            Vector3 direction = new Vector3(camera.getDirectionX(), camera.getDirectionY(), camera.getDirectionZ());
            direction.rotateRad(new Vector3(0, 1, 0), -rotateStep);
            camera.setDirectionX(direction.x);
            camera.setDirectionY(direction.y);
            camera.setDirectionZ(direction.z);
        }
        playerEntity.saveComponents(camera, location);

        fpsLogger.log();
        internalGameLoop.processUpdate(System.currentTimeMillis() - startTime);
        renderingEngine.render();
    }

    @Override
    public void dispose() {
        systemContext.shutdown();
    }


}
