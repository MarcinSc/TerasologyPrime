package com.gempukku.terasology.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.AfterComponentUpdated;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.secsy.network.client.ServerEventBus;
import com.gempukku.terasology.graphics.component.CameraComponent;
import com.gempukku.terasology.graphics.environment.event.AfterChunkGeometryCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkGeometryRemoved;
import com.gempukku.terasology.movement.MovementComponent;
import com.gempukku.terasology.movement.MovementController;
import com.gempukku.terasology.movement.MovementRequestEvent;
import com.gempukku.terasology.physics.component.BasicCylinderPhysicsObjectComponent;
import com.gempukku.terasology.time.TimeManager;
import com.gempukku.terasology.utils.tree.DimensionalMap;
import com.gempukku.terasology.utils.tree.SpaceTree;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.chunk.IntLocationKey;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometry;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometryContainer;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometryManager;
import com.gempukku.terasology.world.chunk.geometry.Triangle;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = {"basicPhysics", NetProfiles.CLIENT}, shared = MovementController.class)
public class ClientBasicPhysicsEngine implements LifeCycleSystem, GameLoopListener, MovementController {
    @In
    private GameLoop gameLoop;
    @In
    private ChunkGeometryManager chunkGeometryManager;
    @In
    private EntityIndexManager entityIndexManager;
    @In
    private TimeManager timeManager;
    @In
    private ServerEventBus serverEventBus;

    // meters per second squared
    private float gravity = -9.81f;

    private Map<IntLocationKey, SpaceTree<Triangle>> chunkTriangles = new HashMap<>();
    private EntityIndex physicsObjectIndex;

    private float maxSpeed;
    private float jumpSpeed;

    private Mode mode = Mode.FREE_FALL;

    private float positionX;
    private float positionY;
    private float positionZ;

    private float yaw;
    private float pitch;
    private float horizontalSpeed;
    private float verticalSpeed;

    private final static float walkPadding = 0.01f;
    private final static float stepHeight = 0.55f;

    private final static float maxSimulationStep = 0.02f;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
        physicsObjectIndex = entityIndexManager.addIndexOnComponents(BasicCylinderPhysicsObjectComponent.class, LocationComponent.class, MovementComponent.class);
    }

    @Override
    public void updateMovement(Mode mode, float yaw, float pitch, float speed, float verticalSpeed) {
        this.mode = mode;
        this.yaw = yaw;
        this.pitch = pitch;
        this.horizontalSpeed = speed;
        this.verticalSpeed = verticalSpeed;

        serverEventBus.sendEventToServer(new MovementRequestEvent(positionX, positionY, positionZ, verticalSpeed, speed, yaw));
    }

    @ReceiveEvent
    public void movementSet(AfterComponentAdded event, EntityRef entity, MovementComponent movement, ClientComponent client) {
        maxSpeed = movement.getMaxSpeed();
        jumpSpeed = movement.getJumpSpeed();
    }

    @ReceiveEvent
    public void movementUpdated(AfterComponentUpdated event, EntityRef entity, MovementComponent movement) {
        if (entity.hasComponent(ClientComponent.class)) {
            maxSpeed = movement.getMaxSpeed();
            jumpSpeed = movement.getJumpSpeed();
        }
    }

    @Override
    public float getHorizontalSpeed() {
        return horizontalSpeed;
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public float getVerticalSpeed() {
        return verticalSpeed;
    }

    @Override
    public float getJumpSpeed() {
        return jumpSpeed;
    }

    @Override
    public float getMaximumSpeed() {
        return maxSpeed;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    private Vector3 tmp = new Vector3();

    @Override
    public void update() {
        float remainingSimulationTimeInSeconds = timeManager.getTimeSinceLastUpdate() / 1000f;

        for (EntityRef entityRef : physicsObjectIndex.getEntities()) {
            if (entityRef.hasComponent(ClientComponent.class)) {
                LocationComponent location = entityRef.getComponent(LocationComponent.class);
                SpaceTree<Triangle>[] chunkSector = getChunkSector(location.getWorldId(), positionX, positionY, positionZ);
                if (chunkSector != null) {
                    while (remainingSimulationTimeInSeconds > 0) {
                        float stepLengthInSeconds = Math.min(maxSimulationStep, remainingSimulationTimeInSeconds);
                        remainingSimulationTimeInSeconds -= stepLengthInSeconds;

                        processSimulationStep(stepLengthInSeconds, entityRef, chunkSector);
                    }

                    CameraComponent camera = entityRef.getComponent(CameraComponent.class);
                    MovementComponent movement = entityRef.getComponent(MovementComponent.class);

                    location.setX(positionX);
                    location.setY(positionY);
                    location.setZ(positionZ);

                    float pitchValue = (float) Math.sin(pitch);
                    tmp.set((float) Math.cos(yaw) * (1 - Math.abs(pitchValue)), pitchValue, (float) Math.sin(yaw) * (1 - Math.abs(pitchValue))).nor();
                    camera.setDirectionX(tmp.x);
                    camera.setDirectionY(tmp.y);
                    camera.setDirectionZ(tmp.z);

                    movement.setVerticalSpeed(verticalSpeed);

                    entityRef.saveComponents(location, camera, movement);
                    serverEventBus.sendEventToServer(new MovementRequestEvent(positionX, positionY, positionZ, movement.getVerticalSpeed(), movement.getSpeed(), movement.getYaw()));

                } else {
                    Gdx.app.error(ClientBasicPhysicsEngine.class.getSimpleName(), "Chunks around are not loaded?");
                }

            }
        }
    }

    private void processSimulationStep(float stepLengthInSeconds, EntityRef entityRef, SpaceTree<Triangle>[] chunkSector) {
        if (mode == Mode.WALKING) {
            processSimulationStepForWalking(stepLengthInSeconds, entityRef, chunkSector);
        } else if (mode == Mode.FREE_FALL) {
            processSimulationStepForFreeFall(stepLengthInSeconds, entityRef, chunkSector);
        }
    }

    private void processSimulationStepForFreeFall(float stepLengthInSeconds, EntityRef entityRef, SpaceTree<Triangle>[] chunkSector) {
        Vector3 collisionNormal = new Vector3();

        BasicCylinderPhysicsObjectComponent cylinderObject = entityRef.getComponent(BasicCylinderPhysicsObjectComponent.class);

        // Simulate movement based on our own values
        float timeHorizontalComponent = horizontalSpeed * stepLengthInSeconds;
        verticalSpeed += gravity * stepLengthInSeconds;

        float x = positionX + timeHorizontalComponent * (float) Math.cos(yaw);
        float y = positionY + verticalSpeed * stepLengthInSeconds;
        float z = positionZ + timeHorizontalComponent * (float) Math.sin(yaw);

        Vector3 point = findCollision(chunkSector, positionX, positionY, positionZ, x, y, z, collisionNormal);

        if (point != null) {
            positionX = point.x;
            positionY = point.y;
            positionZ = point.z;
            verticalSpeed = 0;
            mode = Mode.WALKING;
        } else {
            positionX = x;
            positionY = y;
            positionZ = z;
        }
    }

    private void processSimulationStepForWalking(float stepLengthInSeconds, EntityRef entityRef, SpaceTree<Triangle>[] chunkSector) {
        Vector3 collisionNormal = new Vector3();

        BasicCylinderPhysicsObjectComponent cylinderObject = entityRef.getComponent(BasicCylinderPhysicsObjectComponent.class);

        // Simulate movement based on our own values
        float timeHorizontalComponent = horizontalSpeed * stepLengthInSeconds;

        float x = positionX + timeHorizontalComponent * (float) Math.cos(yaw);
        float z = positionZ + timeHorizontalComponent * (float) Math.sin(yaw);

        if (horizontalSpeed != 0) {
            Vector3 point = findCollision(chunkSector, x, positionY + stepHeight + walkPadding, z, x, positionY, z, collisionNormal);

            if (point != null) {
                positionX = point.x;
                positionY = point.y;
                positionZ = point.z;
            } else {
                point = findCollision(chunkSector, positionX, positionY + walkPadding, positionZ, x, positionY, z, collisionNormal);
                if (point != null) {
                    positionX = point.x;
                    positionZ = point.z;
                } else {
                    positionX = x;
                    positionZ = z;
                    mode = Mode.FREE_FALL;
                }
            }
        } else {
            // TODO - check if player is falling
        }
    }

    private WorldBlock block = new WorldBlock();

    private SpaceTree<Triangle>[] getChunkSector(String worldId, float x, float y, float z) {
        block.set(x, y, z);
        SpaceTree<Triangle>[] result = new SpaceTree[blockSector.length];
        for (int i = 0; i < blockSector.length; i++) {
            SpaceTree<Triangle> triangles = chunkTriangles.get(new IntLocationKey(worldId,
                    blockSector[i][0] + block.getChunkX(),
                    blockSector[i][1] + block.getChunkY(),
                    blockSector[i][2] + block.getChunkZ()));
            if (triangles == null)
                return null;
            result[i] = triangles;
        }
        return result;
    }

    private final int[][] blockSector = new int[][]
            {
                    {-1, -1, -1}, {-1, -1, 0}, {-1, -1, 1},
                    {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1},
                    {-1, 1, -1}, {-1, 1, 0}, {-1, 1, 1},
                    {0, -1, -1}, {0, -1, 0}, {0, -1, 1},
                    {0, 0, -1}, {0, 0, 0}, {0, 0, 1},
                    {0, 1, -1}, {0, 1, 0}, {0, 1, 1},
                    {1, -1, -1}, {1, -1, 0}, {1, -1, 1},
                    {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
                    {1, 1, -1}, {1, 1, 0}, {1, 1, 1}
            };

    private Vector3 temp1 = new Vector3();
    private Vector3 temp2 = new Vector3();
    private Vector3 temp3 = new Vector3();
    private Vector3 temp4 = new Vector3();
    private Vector3 temp5 = new Vector3();
    private Vector3 normal = new Vector3();

    private WorldBlock tempWorldBlock = new WorldBlock();

    private Vector3 findCollision(SpaceTree<Triangle>[] chunkSector, float x1, float y1, float z1, float x2, float y2, float z2, Vector3 resultNormal) {
        temp1.set(x1, y1, z1);
        temp2.set(x2, y2, z2);

        tempWorldBlock.set(x1, y1, z1);

        float distance = temp1.dst(temp2);

        int count = 0;
        float lowestDistance = Float.MAX_VALUE;
        Vector3 closestPoint = null;
        for (SpaceTree<Triangle> trianglesInChunk : chunkSector) {
            Collection<DimensionalMap.Entry<Triangle>> nearest = trianglesInChunk.findNearest(new float[]{x1, y1, z1}, 10, Math.max(distance + 1, 2));
            count += nearest.size();
            for (DimensionalMap.Entry<Triangle> triangleEntry : nearest) {
                triangleEntry.value.getVertices(temp3, temp4, temp5, normal);

                Vector3 result = intersectSegmentTriangle(temp1, temp2, temp3, temp4, temp5);
                if (result != null && normal.dot(x2 - x1, y2 - y1, z2 - z1) <= 0) {
                    float dst = result.dst(temp1);
                    if (dst < lowestDistance) {
                        closestPoint = result;
                        lowestDistance = dst;
                        resultNormal.set(normal);
                    }
                }
            }
        }

        Gdx.app.debug(ClientBasicPhysicsEngine.class.getSimpleName(), "At point: " + x1 + "," + y1 + "," + z1 + " found nearest: " + count);

        return closestPoint;
    }

    @ReceiveEvent
    public void chunkGeometryCreated(AfterChunkGeometryCreated chunkGeometryCreated, EntityRef world) {
        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(chunkGeometryCreated.worldId,
                chunkGeometryCreated.x, chunkGeometryCreated.y, chunkGeometryCreated.z);

        ChunkGeometry chunkGeometry = chunkGeometryContainer.getChunkGeometry();
        SpaceTree<Triangle> triangles = new SpaceTree<>(3);

        for (Triangle triangle : chunkGeometry.getTriangles()) {
            triangle.getVertices(temp1, temp2, temp3, normal);
            triangles.add(new float[]{
                    (temp1.x + temp2.x + temp3.x) / 3,
                    (temp1.y + temp2.y + temp3.y) / 3,
                    (temp1.z + temp2.z + temp3.z) / 3}, triangle);
        }
        chunkTriangles.put(new IntLocationKey(chunkGeometryContainer), triangles);
    }

    @ReceiveEvent
    public void chunkGeometryRemoved(BeforeChunkGeometryRemoved chunkGeometryRemoved, EntityRef world) {
        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(chunkGeometryRemoved.worldId,
                chunkGeometryRemoved.x, chunkGeometryRemoved.y, chunkGeometryRemoved.z);

        chunkTriangles.remove(new IntLocationKey(chunkGeometryContainer));
    }

    private static Vector3 intersectSegmentTriangle(Vector3 from, Vector3 to, Vector3 t0, Vector3 t1, Vector3 t2) {
        Ray ray = new Ray(from, new Vector3(to).sub(from));

        Vector3 result = new Vector3();
        boolean intersect = Intersector.intersectRayTriangle(ray, t0, t1, t2, result);
        if (intersect) {
            if (result.dst(from) > to.dst(from))
                return null;
            else
                return result;
        }
        return null;
    }
}
