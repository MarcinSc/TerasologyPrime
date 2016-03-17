package com.gempukku.terasology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.gempukku.secsy.context.SECSyContext;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.component.InternalComponentManager;
import com.gempukku.secsy.entity.game.InternalGameLoop;
import com.gempukku.secsy.network.LocalCommunication;
import com.gempukku.secsy.network.client.RemoteEntityManager;
import com.gempukku.secsy.network.server.ClientManager;
import com.gempukku.terasology.graphics.RenderingEngine;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.time.InternalTimeManager;
import com.gempukku.terasology.world.MultiverseManager;
import com.gempukku.terasology.world.component.LocationComponent;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class TerasologyApplication extends ApplicationAdapter {
    private static final boolean PROFILE = false;

    private SECSyContext serverContext;
    private SECSyContext clientContext;

    private InternalGameLoop clientInternalGameLoop;
    private RenderingEngine renderingEngine;

    private FPSLogger fpsLogger = new FPSLogger();
    private long startTime;

    private RunningServer runningServer;

    @Override
    public void create() {
        Configuration scanBasedOnAnnotations = new ConfigurationBuilder()
                .setScanners(new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forJavaClassPath());

        setupServerContext(scanBasedOnAnnotations);

        setupClientContext(scanBasedOnAnnotations);

        clientInternalGameLoop = clientContext.getSystem(InternalGameLoop.class);
        renderingEngine = clientContext.getSystem(RenderingEngine.class);

        EntityRef playerEntity = serverContext.getSystem(PlayerManager.class).getPlayer("clientId");

        // Linking the two contexts with LocalCommunication
        LocalCommunication localCommunication = new LocalCommunication(serverContext.getSystem(InternalComponentManager.class), playerEntity);
        serverContext.getSystem(ClientManager.class).addClient("clientId", playerEntity, localCommunication);
        ((RemoteEntityManager) clientContext.getSystem(EntityManager.class)).setServerCommunication(localCommunication);

        runningServer = new RunningServer(
                serverContext.getSystem(InternalGameLoop.class),
                serverContext.getSystem(InternalTimeManager.class));
        runningServer.start();

        if (PROFILE)
            GLProfiler.enable();
    }

    private void setupClientContext(Configuration scanBasedOnAnnotations) {
        Set<String> clientProfiles = new HashSet<>();
        // This is a client context
        clientProfiles.add(NetProfiles.CLIENT);
        // We use components with naming convention
        clientProfiles.add("nameConventionComponents");
        // Client should generate chunk meshes
        clientProfiles.add("generateChunkMeshes");
        // Player controls movement with keyboard
        clientProfiles.add("keyboardController");

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
        // We use components with naming convention
        serverProfiles.add("nameConventionComponents");
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
        playerCamera.setFar(200f);
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
        fpsLogger.log();

        clientInternalGameLoop.processUpdate();

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
    public void resize(int width, int height) {
        renderingEngine.updateCamera();
    }

    @Override
    public void dispose() {
        runningServer.stopServer();
        try {
            runningServer.join();
        } catch (InterruptedException exp) {
            // ignore
        }

        serverContext.shutdown();
        clientContext.shutdown();
    }

    private class RunningServer extends Thread {
        private volatile boolean running = true;
        private InternalTimeManager internalTimeManager;
        private InternalGameLoop internalGameLoop;
        private LinkedList<Runnable> tasksToExecute = new LinkedList<>();

        public RunningServer(InternalGameLoop internalGameLoop, InternalTimeManager internalTimeManager) {
            this.internalGameLoop = internalGameLoop;
            this.internalTimeManager = internalTimeManager;
        }

        public void executeInServerThread(Runnable runnable) {
            tasksToExecute.add(runnable);
        }

        public void stopServer() {
            running = false;
        }

        public void run() {
            long lastUpdate = System.currentTimeMillis();
            while (running) {
                while (true) {
                    Runnable task = tasksToExecute.poll();
                    if (task == null)
                        break;
                    else
                        task.run();
                }

                long newTime = System.currentTimeMillis();
                // The biggest difference between two ticks can be 1000
                long timeDiff = Math.min(1000, newTime - lastUpdate);
                lastUpdate = newTime;

                internalTimeManager.updateMultiverseTime(timeDiff);
                internalGameLoop.processUpdate();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException exp) {
                    // ignore
                }
            }
        }
    }
}
