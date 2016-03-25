package com.gempukku.terasology.physics;

import com.gempukku.secsy.context.annotation.RegisterSystem;

@RegisterSystem(
        profiles = "physics")
public class BulletPhysicsEngine { //implements LifeCycleSystem, GameLoopListener {
//    private final static short GROUND_FLAG = 1 << 8;
//    private final static short OBJECT_FLAG = 1 << 9;
//    private final static short ALL_FLAG = -1;
//
//    @In
//    private ChunkGeometryManager chunkGeometryManager;
//    @In
//    private EntityManager entityManager;
//    @In
//    private GameLoop gameLoop;
//    @In
//    private TimeManager timeManager;
//
//    private Map<String, btCollisionObject> worldGroundCollisionObjects = new HashMap<>();
//    private Map<String, btCompoundShape> worldGroundShapes = new HashMap<>();
//    private Multimap<String, ChunkShapeHolder> chunkShapesPerWorld = HashMultimap.create();
//
//    private btDefaultCollisionConfiguration collisionConfig;
//    private btCollisionDispatcher dispatcher;
//    private btDbvtBroadphase broadphase;
//    private btSequentialImpulseConstraintSolver constraintSolver;
//    private Map<String, btDiscreteDynamicsWorld> dynamicsWorlds = new HashMap<>();
//
//    private Multimap<String, btRigidBody> notAddedBody = HashMultimap.create();
//
//    private int itemIndex = 0;
//    private Map<String, MovingObjectHolder> movingCollisionShapes = new HashMap<>();
//
//    @Override
//    public void preInitialize() {
//        Bullet.init();
//
//        // TODO Add dispose method for these in the system
//        collisionConfig = new btDefaultCollisionConfiguration();
//        dispatcher = new btCollisionDispatcher(collisionConfig);
//        broadphase = new btDbvtBroadphase();
//        constraintSolver = new btSequentialImpulseConstraintSolver();
//    }
//
//    @Override
//    public void initialize() {
//        gameLoop.addGameLoopListener(this);
//    }
//
//    @Override
//    public void update() {
//        float deltaInSeconds = timeManager.getTimeSinceLastUpdate() / 1000f;
//        for (btDiscreteDynamicsWorld dynamicsWorld : dynamicsWorlds.values()) {
//            dynamicsWorld.stepSimulation(deltaInSeconds, 5, 1f / 60f);
//        }
//
//        // Update location of all objects
//        Matrix4 transformMatrix = new Matrix4();
//        Vector3 vector = new Vector3();
//        for (MovingObjectHolder movingObjectHolder : movingCollisionShapes.values()) {
//            movingObjectHolder.rigidBody.getWorldTransform(transformMatrix);
//            vector.set(0, 0, 0);
//            Vector3 position = transformMatrix.getTranslation(vector);
//
//            LocationComponent location = movingObjectHolder.entityRef.getComponent(LocationComponent.class);
//            PhysicsObjectComponent physicsObject = movingObjectHolder.entityRef.getComponent(PhysicsObjectComponent.class);
//            location.setX(position.x - physicsObject.getTranslateFromLocationX());
//            location.setY(position.y - physicsObject.getTranslateFromLocationY());
//            location.setZ(position.z - physicsObject.getTranslateFromLocationZ());
//
//            movingObjectHolder.entityRef.saveComponents(location);
//        }
//    }
//
//    @ReceiveEvent
//    public void afterPlayerCreated(AfterPlayerCreatedEvent event, EntityRef entity) {
//        PhysicsObjectComponent playerPhysicalProperties = entity.createComponent(PhysicsObjectComponent.class);
//        playerPhysicalProperties.setShape("cylinder");
//        playerPhysicalProperties.setParameters("0.8,1.90");
//        playerPhysicalProperties.setMass(80);
//        // Translation from position (on ground) to center of cylinder
//        playerPhysicalProperties.setTranslateFromLocationX(0f);
//        playerPhysicalProperties.setTranslateFromLocationY(0.95f);
//        playerPhysicalProperties.setTranslateFromLocationZ(0f);
//        entity.saveComponents(playerPhysicalProperties);
//    }
//
//    @ReceiveEvent
//    public void afterPhysicsObjectAdded(AfterComponentAdded event, EntityRef entity, PhysicsObjectComponent physicsObject, LocationComponent location) {
//        if (physicsObject.getShape().equals("cylinder")) {
//            String[] parameters = physicsObject.getParameters().split(",");
//            float diameter = Float.parseFloat(parameters[0]);
//            float height = Float.parseFloat(parameters[1]);
//            btCylinderShape shape = new btCylinderShape(new Vector3(diameter / 2, height / 2, diameter / 2));
//
//            MovingObjectHolder objectHolder = new MovingObjectHolder(itemIndex++, entity, shape, physicsObject.getMass());
//
//            Matrix4 moveMatrix = new Matrix4().translate(
//                    location.getX() + physicsObject.getTranslateFromLocationX(),
//                    location.getY() + physicsObject.getTranslateFromLocationY(),
//                    location.getZ() + physicsObject.getTranslateFromLocationZ());
//            objectHolder.rigidBody.setWorldTransform(moveMatrix);
//
//            movingCollisionShapes.put(entityManager.getEntityUniqueIdentifier(entity), objectHolder);
//
//            btDiscreteDynamicsWorld discreteDynamicsWorld = dynamicsWorlds.get(location.getWorldId());
//            if (discreteDynamicsWorld != null)
//                discreteDynamicsWorld.addRigidBody(objectHolder.rigidBody, OBJECT_FLAG, GROUND_FLAG);
//            else
//                notAddedBody.put(location.getWorldId(), objectHolder.rigidBody);
//        }
//    }
//
//    @ReceiveEvent
//    public void beforePhysicsObjectRemoved(BeforeComponentRemoved event, EntityRef entity, PhysicsObjectComponent physicsObject, LocationComponent location) {
//        String uniqueId = entityManager.getEntityUniqueIdentifier(entity);
//        MovingObjectHolder objectHolder = movingCollisionShapes.remove(uniqueId);
//        btDiscreteDynamicsWorld discreteDynamicsWorld = dynamicsWorlds.get(location.getWorldId());
//        if (discreteDynamicsWorld != null)
//            discreteDynamicsWorld.removeRigidBody(objectHolder.rigidBody);
//        else
//            notAddedBody.remove(location.getWorldId(), objectHolder.rigidBody);
//
//        objectHolder.dispose();
//    }
//
//    @ReceiveEvent
//    public void afterWorldCreated(AfterComponentAdded event, EntityRef worldEntity, WorldComponent world) {
//        String worldId = world.getWorldId();
//
//        btCompoundShape groundShape = new btCompoundShape();
//        worldGroundShapes.put(worldId, groundShape);
//
//        btCollisionObject groundCollisionObject = new btCollisionObject();
//        groundCollisionObject.setCollisionShape(groundShape);
//
//        worldGroundCollisionObjects.put(worldId, groundCollisionObject);
//
//        btDiscreteDynamicsWorld dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
//        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
//        dynamicsWorld.addCollisionObject(groundCollisionObject, GROUND_FLAG, ALL_FLAG);
//
//        dynamicsWorlds.put(worldId, dynamicsWorld);
//
//        for (btRigidBody btRigidBody : notAddedBody.removeAll(worldId)) {
//            dynamicsWorld.addRigidBody(btRigidBody, OBJECT_FLAG, GROUND_FLAG);
//        }
//    }
//
//    @ReceiveEvent
//    public void beforeWorldRemoved(BeforeComponentRemoved event, EntityRef worldEntity, WorldComponent world) {
//        String worldId = world.getWorldId();
//
//        btDiscreteDynamicsWorld dynamicsWorld = dynamicsWorlds.remove(worldId);
//        dynamicsWorld.dispose();
//
//        btCollisionObject groundCollisionObject = worldGroundCollisionObjects.remove(worldId);
//        groundCollisionObject.dispose();
//        btCompoundShape groundShape = worldGroundShapes.remove(worldId);
//        groundShape.dispose();
//    }
//
//    @ReceiveEvent
//    public void chunkLoaded(AfterChunkGeometryCreated event, EntityRef worldEntity, WorldComponent world) {
//        String worldId = world.getWorldId();
//
//        ChunkGeometryContainer chunkGeometryContainer = chunkGeometryManager.getChunkGeometry(worldId, event.x, event.y, event.z);
//        Array<MeshPart> meshes = new Array<>();
//        for (MeshPart meshPart : chunkGeometryContainer.getMeshParts()) {
//            if (meshPart != null)
//                meshes.add(meshPart);
//        }
//        if (meshes.size > 0) {
//            btBvhTriangleMeshShape chunkShape = btBvhTriangleMeshShape.obtain(meshes);
//            chunkShapesPerWorld.put(worldId, new ChunkShapeHolder(chunkShape, event.x, event.y, event.z));
//
//            // Integrate chunk into world shape
//            btCompoundShape groundShape = worldGroundShapes.get(worldId);
//            groundShape.addChildShape(new Matrix4(), chunkShape);
//        }
//    }
//
//    @ReceiveEvent
//    public void chunkUnloaded(BeforeChunkGeometryRemoved event, EntityRef worldEntity, WorldComponent world) {
//        String worldId = world.getWorldId();
//
//        ChunkShapeHolder chunkShapeHolder = findChunkShapeHolder(worldId, event.x, event.y, event.z);
//        if (chunkShapeHolder != null) {
//            chunkShapesPerWorld.remove(worldId, chunkShapeHolder);
//            btCollisionShape chunkShape = chunkShapeHolder.chunkShape;
//            worldGroundShapes.get(worldId).removeChildShape(chunkShape);
//            chunkShape.dispose();
//        }
//    }
//
//    private ChunkShapeHolder findChunkShapeHolder(String worldId, int x, int y, int z) {
//        Collection<ChunkShapeHolder> chunkShapeHolders = chunkShapesPerWorld.get(worldId);
//        for (ChunkShapeHolder chunkShapeHolder : chunkShapeHolders) {
//            if (chunkShapeHolder.x == x && chunkShapeHolder.y == y && chunkShapeHolder.z == z)
//                return chunkShapeHolder;
//        }
//        return null;
//    }
}
