package com.gempukku.terasology.physics;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.dispatch.ReceiveEvent;
import com.gempukku.secsy.entity.event.AfterComponentAdded;
import com.gempukku.secsy.entity.event.BeforeComponentRemoved;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.chunk.event.AfterChunkLoadedEvent;
import com.gempukku.terasology.world.component.ShapeAndTextureComponent;
import com.gempukku.terasology.world.component.WorldComponent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem
public class PhysicsEngine implements LifeCycleSystem {
    @In
    private ChunkBlocksProvider chunkBlocksProvider;
    @In
    private CommonBlockManager commonBlockManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;
    @In
    private ShapeProvider shapeProvider;

    private Map<String, btCollisionObject> worldGroundCollisionObjects = new HashMap<>();
    private Map<String, btCompoundShape> worldGroundShapes = new HashMap<>();
    private Multimap<String, ChunkShapeHolder> chunkShapesPerWorld = HashMultimap.create();
    private String shapeAndTextureComponentName;

    @Override
    public void preInitialize() {
        Bullet.init();
    }

    @Override
    public void initialize() {
        shapeAndTextureComponentName = terasologyComponentManager.getNameByComponent(ShapeAndTextureComponent.class);
    }

    @ReceiveEvent
    public void afterWorldCreated(AfterComponentAdded event, EntityRef worldEntity, WorldComponent world) {
        String worldId = world.getWorldId();

        btCompoundShape groundShape = new btCompoundShape();
        worldGroundShapes.put(worldId, groundShape);

        btCollisionObject groundCollisionObject = new btCollisionObject();
        groundCollisionObject.setCollisionShape(groundShape);

        worldGroundCollisionObjects.put(worldId, groundCollisionObject);
    }

    @ReceiveEvent
    public void beforeWorldRemoved(BeforeComponentRemoved event, EntityRef worldEntity, WorldComponent world) {
        String worldId = world.getWorldId();
        btCollisionObject groundCollisionObject = worldGroundCollisionObjects.remove(worldId);
        groundCollisionObject.dispose();
        btCompoundShape groundShape = worldGroundShapes.remove(worldId);
        groundShape.dispose();
    }

    @ReceiveEvent
    public void chunkLoaded(AfterChunkLoadedEvent event, EntityRef worldEntity, WorldComponent world) {
        String worldId = world.getWorldId();

        btCompoundShape chunkShape = new btCompoundShape();
        ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, event.x, event.y, event.z);
        addChunkToShape(chunkBlocks, chunkShape);
        chunkShapesPerWorld.put(worldId, new ChunkShapeHolder(chunkShape, event.x, event.y, event.z));

        // Integrate chunk into world shape
        btCompoundShape groundShape = worldGroundShapes.get(worldId);
        groundShape.addChildShape(new Matrix4(), chunkShape);
    }

    private void addChunkToShape(ChunkBlocks chunkBlocks, btCompoundShape chunkShape) {
        for (int dx = 0; dx < ChunkSize.X; dx++) {
            for (int dy = 0; dy < ChunkSize.Y; dy++) {
                for (int dz = 0; dz < ChunkSize.Z; dz++) {
                    String commonBlockAt = chunkBlocks.getCommonBlockAt(dx, dy, dz);
                    if (hasTextureAndShape(commonBlockAt)) {
//                        btConvexTriangleMeshShape blockMeshShape = new btConvexTriangleMeshShape()
//
//                        String shapeId = getShapeId(commonBlockAt);
//                        ShapeDef shape = shapeProvider.getShapeById(shapeId);
//                        for (ShapePartDef shapePart : shape.getShapeParts()) {
//                            shapePart.
//                        }
                    }
                }
            }
        }
    }

    private String getShapeId(String commonBlockId) {
        return (String) commonBlockManager.getCommonBlockById(commonBlockId).getComponents().
                get(shapeAndTextureComponentName).getFields().get("shapeId");
    }

    private boolean hasTextureAndShape(String commonBlockId) {
        return commonBlockManager.getCommonBlockById(commonBlockId).getComponents().
                get(shapeAndTextureComponentName) != null;
    }

}
