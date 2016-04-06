package com.gempukku.terasology.landd;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.secsy.entity.io.StoredEntityData;
import com.gempukku.secsy.network.serialize.ComponentInformation;
import com.gempukku.secsy.network.serialize.EntityInformation;
import com.gempukku.terasology.communication.SendToClientComponent;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.faction.FactionComponent;
import com.gempukku.terasology.faction.FactionMemberComponent;
import com.gempukku.terasology.landd.component.AiCharacterComponent;
import com.gempukku.terasology.landd.component.FactionObjectComponent;
import com.gempukku.terasology.landd.component.MovingCharacterComponent;
import com.gempukku.terasology.landd.component.PermanentChunkLoadingComponent;
import com.gempukku.terasology.landd.component.RangedAttackCharacterComponent;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.procedural.FastRandom;
import com.gempukku.terasology.procedural.Noise;
import com.gempukku.terasology.procedural.SimplexNoise;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkComponent;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.chunk.WorldGenerator;
import com.gempukku.terasology.world.component.LocationComponent;
import com.gempukku.terasology.world.component.MultiverseComponent;
import com.gempukku.terasology.world.component.WorldComponent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@RegisterSystem(
        profiles = {NetProfiles.AUTHORITY, "lAndDWorld"}, shared = WorldGenerator.class)
public class LAndDWorldGenerator implements WorldGenerator {
    @In
    private PrefabManager prefabManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;
    @In
    private CommonBlockManager commonBlockManager;

    private float noiseScale = 0.001f;
    private Noise noise = new SimplexNoise(0);

    private short air = -1;
    private short grass = -1;
    private short dirt = -1;
    private short stone = -1;
    private short tree = -1;

    private EntityData oakPrefab;
    private EntityData pinePrefab;

    private int worldSizeChunks = 10;
    private int baseSize = 20;
    private int evenTerrainSize = 10;
    private int evenTerrainSoftness = 5 * evenTerrainSize;
    private int mountainAmplitude = 32;

    @Override
    public EntityData createMultiverseEntity() {
        EntityInformation entityInformation = new EntityInformation();

        ComponentInformation multiverse = new ComponentInformation(MultiverseComponent.class);
        multiverse.addField("time", 0L);
        entityInformation.addComponent(multiverse);

        ComponentInformation chunkLoading = new ComponentInformation(PermanentChunkLoadingComponent.class);
        chunkLoading.addField("worldId", "world");

        chunkLoading.addField("minimumChunkX", -2);
        chunkLoading.addField("minimumChunkY", -3);
        chunkLoading.addField("minimumChunkZ", -2);

        chunkLoading.addField("maximumChunkX", worldSizeChunks + 2);
        chunkLoading.addField("maximumChunkY", 3);
        chunkLoading.addField("maximumChunkZ", worldSizeChunks + 2);

        entityInformation.addComponent(chunkLoading);

        return entityInformation;
    }

    @Override
    public Iterable<EntityData> createStartingEntities() {
        return Arrays.asList(
                createFactionEntity("black", "white"),
                createFactionEntity("white", "black"),
                createFactionRangedEntity("white", "world", 10, 1, 10),
                createFactionObjectEntity("black", "world", 50, 1, 50));
    }

    private EntityInformation createFactionRangedEntity(String factionId, String worldId, float x, float y, float z) {
        EntityInformation object = createFactionObjectEntity(factionId, worldId, x, y, z);

        ComponentInformation moving = new ComponentInformation(MovingCharacterComponent.class);
        moving.addField("speedX", 3f);
        moving.addField("speedY", 0f);
        moving.addField("speedZ", 3f);
        object.addComponent(moving);

        ComponentInformation ranged = new ComponentInformation(RangedAttackCharacterComponent.class);
        ranged.addField("firingRange", 10f);
        ranged.addField("firingCooldown", 1000L);
        ranged.addField("lastFired", 0L);
        object.addComponent(ranged);

        return object;
    }

    private EntityInformation createFactionEntity(String factionId, String opposingFactionId) {
        EntityInformation faction = new EntityInformation();
        ComponentInformation factionComp = new ComponentInformation(FactionComponent.class);
        factionComp.addField("factionId", factionId);
        Set<String> opposingFactions = new HashSet<>();
        opposingFactions.add(opposingFactionId);
        factionComp.addField("opposingFactions", opposingFactions);
        faction.addComponent(factionComp);
        return faction;
    }

    private EntityInformation createFactionObjectEntity(String factionId, String worldId, float x, float y, float z) {
        EntityInformation result = new EntityInformation();

        ComponentInformation factionComp = new ComponentInformation(FactionMemberComponent.class);
        factionComp.addField("factionId", factionId);
        result.addComponent(factionComp);

        ComponentInformation factionObject = new ComponentInformation(FactionObjectComponent.class);
        result.addComponent(factionObject);

        ComponentInformation location = new ComponentInformation(LocationComponent.class);
        location.addField("worldId", worldId);
        location.addField("x", x);
        location.addField("y", y);
        location.addField("z", z);
        result.addComponent(location);

        ComponentInformation ai = new ComponentInformation(AiCharacterComponent.class);
        result.addComponent(ai);

        ComponentInformation sendToClient = new ComponentInformation(SendToClientComponent.class);
        result.addComponent(sendToClient);

        return result;
    }

    @Override
    public EntityData createWorldEntity(String worldId) {
        EntityInformation entityInformation = new EntityInformation();

        int dayLength = 30 * 60 * 1000;

        ComponentInformation world = new ComponentInformation(WorldComponent.class);
        world.addField("worldId", worldId);
        world.addField("dayLength", dayLength);
        // Start just after sunrise
        world.addField("dayStartDifferenceFromMultiverse", dayLength / 4);
        entityInformation.addComponent(world);

        return entityInformation;
    }

    @Override
    public Iterable<StoredEntityData> generateChunk(String worldId, int x, int y, int z) {
        if (air == -1) {
            air = commonBlockManager.getCommonBlockId("air");
            grass = commonBlockManager.getCommonBlockId("grass");
            dirt = commonBlockManager.getCommonBlockId("dirt");
            stone = commonBlockManager.getCommonBlockId("stone");
            tree = commonBlockManager.getCommonBlockId("tree");

            oakPrefab = prefabManager.getPrefabByName("oak");
            pinePrefab = prefabManager.getPrefabByName("pine");
        }

        FastRandom rnd = new FastRandom(x + 153 * y + 3121 * z);

        List<StoredEntityData> entities = new LinkedList<>();
        short[] blockIds = new short[ChunkSize.X * ChunkSize.Y * ChunkSize.Z];
        if (x < 0 || x >= worldSizeChunks || z < 0 || z >= worldSizeChunks) {
            for (int i = 0; i < blockIds.length; i++) {
                blockIds[i] = air;
            }
        } else {
            int index = 0;
            for (int dx = 0; dx < ChunkSize.X; dx++) {
                for (int dy = 0; dy < ChunkSize.Y; dy++) {
                    int blockLevel = y * ChunkSize.Y + dy;
                    for (int dz = 0; dz < ChunkSize.Z; dz++) {
                        int worldX = x * ChunkSize.X + dx;
                        int worldZ = z * ChunkSize.Z + dz;

                        float noiseForColumn = this.noise.noise(noiseScale * worldX, noiseScale * worldZ);
                        noiseForColumn = (noiseForColumn + 1 / 2);
                        int groundLevel = getGroundLevel(mountainAmplitude, noiseForColumn,
                                x * ChunkSize.X + dx, z * ChunkSize.Z + dz);
//                        if (blockLevel == groundLevel + 1 && dx % (ChunkSize.X / 2) == 0 && dz % (ChunkSize.Z / 2) == 0) {
//                            EntityData prefab = (dx == 0) ? oakPrefab : pinePrefab;
//                            int maxGenerations = ((Number) prefab.getComponent(SimpleTreeDefinitionComponent.class).getFields().get("maxGenerations")).intValue();
//                            EntityInformation entityInformation = new EntityInformation(prefab);
//
//                            ComponentInformation individual = new ComponentInformation(IndividualTreeComponent.class);
//                            individual.addField("generation", rnd.nextInt(maxGenerations) + 1);
//                            entityInformation.addComponent(individual);
//
//                            ComponentInformation seed = new ComponentInformation(SeedComponent.class);
//                            seed.addField("seed", rnd.nextLong());
//                            entityInformation.addComponent(seed);
//
//                            ComponentInformation block = new ComponentInformation(BlockComponent.class);
//                            entityInformation.addComponent(block);
//
//                            ComponentInformation location = new ComponentInformation(LocationComponent.class);
//                            location.addField("worldId", worldId);
//                            location.addField("x", (float) worldX);
//                            location.addField("y", (float) (y * ChunkSize.Y + dy));
//                            location.addField("z", (float) worldZ);
//                            entityInformation.addComponent(location);
//
//                            entities.add(entityInformation);
//                            blockIds[index] = tree;
//                        } else if (blockLevel > groundLevel) {
                        if (blockLevel>groundLevel) {
                            blockIds[index] = air;
                        } else if (blockLevel == groundLevel) {
                            blockIds[index] = grass;
                        } else if (blockLevel > groundLevel - 3) {
                            blockIds[index] = dirt;
                        } else {
                            blockIds[index] = stone;
                        }
                        index++;
                    }
                }
            }
        }

        EntityInformation chunkEntity = new EntityInformation();
        ComponentInformation chunk = new ComponentInformation(ChunkComponent.class);
        chunk.addField("worldId", worldId);
        chunk.addField("x", x);
        chunk.addField("y", y);
        chunk.addField("z", z);
        chunk.addField("blockIds", blockIds);
        chunkEntity.addComponent(chunk);
        entities.add(chunkEntity);

        return entities;
    }

    private int getGroundLevel(int mountainAmplitude, float noiseForColumn, int x, int z) {
        int distanceFromEdge =
                Math.min(Math.min(x, z), Math.min(worldSizeChunks * ChunkSize.X - x, worldSizeChunks * ChunkSize.Z - z));
        int distanceFromDiagonal = Math.abs(x - z);

        int distanceFromForcedEvenTerrain = Math.min(distanceFromEdge, distanceFromDiagonal);

        float distanceFromCorner = (float) Math.sqrt(Math.min(x * x + z * z, (worldSizeChunks * ChunkSize.X - x) * (worldSizeChunks * ChunkSize.X - x) + (worldSizeChunks * ChunkSize.Z - z) * (worldSizeChunks * ChunkSize.Z - z)));

        float multiplierFromLanes = (distanceFromForcedEvenTerrain < evenTerrainSize) ? 0 : Math.min((distanceFromForcedEvenTerrain - evenTerrainSize) * 1f / evenTerrainSoftness, 1f);
        float multiplierFromBase = (distanceFromCorner < baseSize) ? 0 : Math.min((distanceFromCorner - baseSize) * 1f / evenTerrainSoftness, 1f);

        float multiplier = Math.min(multiplierFromLanes, multiplierFromBase);

        return Math.round(multiplier * FastMath.floor(noiseForColumn * mountainAmplitude));
    }
}
