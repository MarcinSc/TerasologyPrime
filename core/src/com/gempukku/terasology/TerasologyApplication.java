package com.gempukku.terasology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.SECSyContext;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.entity.network.LocalCommunication;
import com.gempukku.secsy.entity.network.client.RemoteEntityManager;
import com.gempukku.secsy.entity.network.server.ClientManager;
import com.gempukku.terasology.component.LocationComponent;
import com.gempukku.terasology.graphics.RenderingEngine;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.world.MultiverseManager;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.Set;

public class TerasologyApplication extends ApplicationAdapter {
    private static final boolean PROFILE = false;

    private SECSyContext serverContext;
    private SECSyContext clientContext;

    private InternalGameLoop serverInternalGameLoop;
    private InternalGameLoop clientInternalGameLoop;
    private RenderingEngine renderingEngine;

    private FPSLogger fpsLogger = new FPSLogger();
    private long startTime;

    @Override
    public void create() {
        Configuration scanBasedOnAnnotations = new ConfigurationBuilder()
                .setScanners(new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forJavaClassPath());

        setupServerContext(scanBasedOnAnnotations);

        setupClientContext(scanBasedOnAnnotations);

        serverInternalGameLoop = serverContext.getSystem(InternalGameLoop.class);
        clientInternalGameLoop = clientContext.getSystem(InternalGameLoop.class);
        renderingEngine = clientContext.getSystem(RenderingEngine.class);

        EntityRef playerEntity = serverContext.getSystem(PlayerManager.class).getPlayer("clientId");

        // Linking the two contexts with LocalCommunication
        LocalCommunication localCommunication = new LocalCommunication(serverContext.getSystem(InternalComponentManager.class), playerEntity);
        serverContext.getSystem(ClientManager.class).addClient("clientId", playerEntity, localCommunication);
        ((RemoteEntityManager) clientContext.getSystem(EntityManager.class)).setServerCommunication(localCommunication);

        if (PROFILE)
            GLProfiler.enable();
    }

    private void setupClientContext(Configuration scanBasedOnAnnotations) {
        Set<String> clientProfiles = new HashSet<>();
        // This is a client context
        clientProfiles.add(NetProfiles.CLIENT);
        // We use components created in map
        clientProfiles.add("mapComponent");

        clientContext = new SECSyContext(clientProfiles, new Reflections(scanBasedOnAnnotations));
        clientContext.startup();

        System.out.println("List of systems in client context:");
        for (Object system : clientContext.getSystems()) {
            System.out.println(system.getClass().getSimpleName());
        }
    }

    private void setupServerContext(Configuration scanBasedOnAnnotations) {
        Set<String> serverProfiles = new HashSet<>();
        // This is server context
        serverProfiles.add(NetProfiles.AUTHORITY);
        // We use components created in map
        serverProfiles.add("mapComponent");
        // World generator
        serverProfiles.add("hillsWorld");

        serverContext = new SECSyContext(serverProfiles, new Reflections(scanBasedOnAnnotations));
        serverContext.startup();

        System.out.println("List of systems in server context:");
        for (Object system : serverContext.getSystems()) {
            System.out.println(system.getClass().getSimpleName());
        }

        MultiverseManager multiverseManager = serverContext.getSystem(MultiverseManager.class);
        multiverseManager.createWorld("world");

        PlayerManager playerManager = serverContext.getSystem(PlayerManager.class);
        EntityRef player = playerManager.createPlayer("clientId");

        LocationComponent playerLocation = player.createComponent(LocationComponent.class);
        playerLocation.setX(0);
        playerLocation.setY(0);
        playerLocation.setZ(0);
        playerLocation.setWorldId("world");

        CameraComponent playerCamera = player.createComponent(CameraComponent.class);
        playerCamera.setNear(0.1f);
        playerCamera.setFar(300f);
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
        EntityRef playerEntity = serverContext.getSystem(PlayerManager.class).getPlayer("clientId");
        CameraComponent camera = playerEntity.getComponent(CameraComponent.class);
        LocationComponent location = playerEntity.getComponent(LocationComponent.class);

        float rotateStep = 0.05f;
        float stepLength = 0.5f;

        boolean changed = false;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            location.setX(location.getX()+camera.getDirectionX()*stepLength);
            location.setY(location.getY()+camera.getDirectionY()*stepLength);
            location.setZ(location.getZ()+camera.getDirectionZ()*stepLength);
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            location.setX(location.getX()-camera.getDirectionX()*stepLength);
            location.setY(location.getY()-camera.getDirectionY()*stepLength);
            location.setZ(location.getZ()-camera.getDirectionZ()*stepLength);
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            location.setY(location.getY()+stepLength);
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
            location.setY(location.getY()-stepLength);
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            location.setX(location.getX()-camera.getDirectionX()*stepLength);
            location.setY(location.getY()-camera.getDirectionY()*stepLength);
            location.setZ(location.getZ()-camera.getDirectionZ()*stepLength);
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            Vector3 direction = new Vector3(camera.getDirectionX(), camera.getDirectionY(), camera.getDirectionZ());
            direction.rotateRad(new Vector3(0, 1, 0), rotateStep);
            camera.setDirectionX(direction.x);
            camera.setDirectionY(direction.y);
            camera.setDirectionZ(direction.z);
            changed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            Vector3 direction = new Vector3(camera.getDirectionX(), camera.getDirectionY(), camera.getDirectionZ());
            direction.rotateRad(new Vector3(0, 1, 0), -rotateStep);
            camera.setDirectionX(direction.x);
            camera.setDirectionY(direction.y);
            camera.setDirectionZ(direction.z);
            changed = true;
        }
        if (changed)
            playerEntity.saveComponents(camera, location);

        fpsLogger.log();

        serverInternalGameLoop.processUpdate(System.currentTimeMillis() - startTime);
        clientInternalGameLoop.processUpdate(System.currentTimeMillis() - startTime);

        renderingEngine.render();
        if (PROFILE) {
            System.out.println("Texture bindings: " + GLProfiler.textureBindings);
            System.out.println("Draw calls: " + GLProfiler.drawCalls);
            System.out.println("Vertices: " + GLProfiler.vertexCount.total);
            System.out.println("Shader switches: " + GLProfiler.shaderSwitches);

            GLProfiler.reset();
        }
    }

    @Override
    public void dispose() {
        serverContext.shutdown();
        clientContext.shutdown();
    }


}
