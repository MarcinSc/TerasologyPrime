package com.gempukku.terasology.physics;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.terasology.graphics.environment.event.AfterChunkGeometryCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkGeometryRemoved;
import com.gempukku.terasology.movement.MovementComponent;
import com.gempukku.terasology.movement.MovementController;
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
import com.gempukku.terasology.world.component.LocationComponent;
import com.gempukku.terasology.world.event.AfterPlayerCreatedEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = {"basicPhysics", NetProfiles.AUTHORITY}, shared = MovementController.class)
public class AuthorityBasicPhysicsEngine implements LifeCycleSystem, GameLoopListener {
    @In
    private GameLoop gameLoop;
    @In
    private ChunkGeometryManager chunkGeometryManager;
    @In
    private EntityIndexManager entityIndexManager;
    @In
    private TimeManager timeManager;

    private float gravity = -9.6f;

    private Map<IntLocationKey, SpaceTree<Triangle>> chunkTriangles = new HashMap<>();
    private EntityIndex physicsObjectIndex;

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
        physicsObjectIndex = entityIndexManager.addIndexOnComponents(BasicCylinderPhysicsObjectComponent.class, LocationComponent.class, MovementComponent.class);
    }

    private Vector3 tmp = new Vector3();

    @Override
    public void update() {
//        float timeSinceLastUpdateInSeconds = timeManager.getTimeSinceLastUpdate() / 1000f;
//
//        for (EntityRef entityRef : physicsObjectIndex.getEntities()) {
//            LocationComponent location = entityRef.getComponent(LocationComponent.class);
//            if (isChunksAroundLoaded(location.getWorldId(), location.getX(), location.getY(), location.getZ())) {
//                CameraComponent camera = entityRef.getComponent(CameraComponent.class);
//                MovementComponent movement = entityRef.getComponent(MovementComponent.class);
//
//                float speed = movement.getSpeed();
//                float timeHorizontalComponent = speed * timeSinceLastUpdateInSeconds;
//                float verticalSpeed = movement.getVerticalSpeed() + gravity * timeSinceLastUpdateInSeconds;
//
//                float x = location.getX() + timeHorizontalComponent * (float) Math.cos(movement.getYaw());
//                float y = location.getY() + verticalSpeed * timeSinceLastUpdateInSeconds;
//                float z = location.getZ() + timeHorizontalComponent * (float) Math.sin(movement.getYaw());
//
//                Vector3 point = findCollision(location.getWorldId(), location.getX(), location.getY(), location.getZ(), x, y, z);
//
//                if (point != null) {
//                    x = point.x;
//                    y = point.y;
//                    z = point.z;
//                    verticalSpeed = 0;
//                }
//                location.setX(x);
//                location.setY(y);
//                location.setZ(z);
//
//                movement.setVerticalSpeed(verticalSpeed);
//
//                entityRef.saveComponents(location, camera, movement);
//            }
//        }
    }

    private WorldBlock block = new WorldBlock();

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

    private boolean isChunksAroundLoaded(String worldId, float x, float y, float z) {
        block.set(x, y, z);
        for (int[] diff : blockSector) {
            if (!isChunkLoaded(worldId,
                    diff[0] + block.getChunkX(),
                    diff[1] + block.getChunkY(),
                    diff[2] + block.getChunkZ()))
                return false;
        }
        return true;
    }

    private boolean isChunkLoaded(String worldId, int x, int y, int z) {
        return chunkTriangles.get(new IntLocationKey(worldId, x, y, z)) != null;
    }

    private Vector3 temp1 = new Vector3();
    private Vector3 temp2 = new Vector3();
    private Vector3 temp3 = new Vector3();
    private Vector3 temp4 = new Vector3();
    private Vector3 temp5 = new Vector3();
    private Vector3 normal = new Vector3();

    private WorldBlock tempWorldBlock = new WorldBlock();

    private Vector3 findCollision(String worldId, float x1, float y1, float z1, float x2, float y2, float z2) {
        temp1.set(x1, y1, z1);
        temp2.set(x2, y2, z2);

        tempWorldBlock.set(x1, y1, z1);

        float distance = temp1.dst(temp2);

        float lowestDistance = Float.MAX_VALUE;
        Vector3 closestPoint = null;
        for (int[] diff : blockSector) {
            SpaceTree<Triangle> trianglesInChunk = chunkTriangles.get(new IntLocationKey(worldId,
                    diff[0] + tempWorldBlock.getChunkX(),
                    diff[1] + tempWorldBlock.getChunkY(),
                    diff[2] + tempWorldBlock.getChunkZ()));

            Collection<DimensionalMap.Entry<Triangle>> nearest = trianglesInChunk.findNearest(new float[]{temp1.x, temp1.y, temp1.z}, 100, distance + 3);
            for (DimensionalMap.Entry<Triangle> triangleEntry : nearest) {
                triangleEntry.value.getVertices(temp3, temp4, temp5, normal);

                Vector3 result = intersectSegmentTriangle(temp1, temp2, temp3, temp4, temp5);
                if (result != null) {
                    float dst = result.dst(temp1);
                    if (dst < lowestDistance) {
                        closestPoint = result;
                        lowestDistance = dst;
                    }
                }
            }
        }

        return closestPoint;
    }

    @ReceiveEvent
    public void afterPlayerCreated(AfterPlayerCreatedEvent event, EntityRef entity) {
        BasicCylinderPhysicsObjectComponent playerPhysicalProperties = entity.createComponent(BasicCylinderPhysicsObjectComponent.class);
        playerPhysicalProperties.setRadius(0.4f);
        playerPhysicalProperties.setHeight(1.9f);
        entity.saveComponents(playerPhysicalProperties);
    }

    @ReceiveEvent
    public void chunkGeometryCreated(AfterChunkGeometryCreated chunkGeometryCreated, EntityRef world) {
        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(chunkGeometryCreated.worldId,
                chunkGeometryCreated.x, chunkGeometryCreated.y, chunkGeometryCreated.z);

        ChunkGeometry chunkGeometry = chunkGeometryContainer.getChunkGeometry();
        SpaceTree<Triangle> triangles = new SpaceTree<>(3);
        chunkTriangles.put(new IntLocationKey(chunkGeometryContainer), triangles);

        for (Triangle triangle : chunkGeometry.getTriangles()) {
            triangle.getVertices(temp1, temp2, temp3, normal);
            triangles.add(new float[]{
                    (temp1.x + temp2.x + temp3.x) / 3,
                    (temp1.y + temp2.y + temp3.y) / 3,
                    (temp1.z + temp2.z + temp3.z) / 3}, triangle);
        }
    }

    @ReceiveEvent
    public void chunkGeometryRemoved(BeforeChunkGeometryRemoved chunkGeometryRemoved, EntityRef world) {
        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(chunkGeometryRemoved.worldId,
                chunkGeometryRemoved.x, chunkGeometryRemoved.y, chunkGeometryRemoved.z);

        ChunkGeometry chunkGeometry = chunkGeometryContainer.getChunkGeometry();
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
