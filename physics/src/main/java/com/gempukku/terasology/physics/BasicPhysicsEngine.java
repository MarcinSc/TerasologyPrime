package com.gempukku.terasology.physics;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.game.GameLoop;
import com.gempukku.secsy.entity.game.GameLoopListener;
import com.gempukku.terasology.graphics.environment.event.AfterChunkGeometryCreated;
import com.gempukku.terasology.graphics.environment.event.BeforeChunkGeometryRemoved;
import com.gempukku.terasology.utils.tree.SpaceTree;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometry;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometryContainer;
import com.gempukku.terasology.world.chunk.geometry.ChunkGeometryManager;
import com.gempukku.terasology.world.chunk.geometry.Triangle;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = "basicPhysics")
public class BasicPhysicsEngine implements LifeCycleSystem, GameLoopListener {
    @In
    private GameLoop gameLoop;
    @In
    private ChunkGeometryManager chunkGeometryManager;

    private Map<ChunkGeometry, SpaceTree<Triangle>> chunkTriangles = new HashMap<>();

    @Override
    public void initialize() {
        gameLoop.addGameLoopListener(this);
    }

    @Override
    public void update() {

    }

    private Vector3 temp1 = new Vector3();
    private Vector3 temp2 = new Vector3();
    private Vector3 temp3 = new Vector3();
    private Vector3 normal = new Vector3();

    @ReceiveEvent
    public void chunkGeometryCreated(AfterChunkGeometryCreated chunkGeometryCreated, EntityRef world) {
        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(chunkGeometryCreated.worldId,
                chunkGeometryCreated.x, chunkGeometryCreated.y, chunkGeometryCreated.z);

        ChunkGeometry chunkGeometry = chunkGeometryContainer.getChunkGeometry();
        SpaceTree<Triangle> triangles = new SpaceTree<>(3);
        chunkTriangles.put(chunkGeometry, triangles);

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
        chunkTriangles.remove(chunkGeometry);
    }
}
