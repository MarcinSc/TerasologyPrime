package com.gempukku.terasology.test;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.secsy.network.serialize.ComponentInformation;
import com.gempukku.secsy.network.serialize.EntityInformation;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.procedural.FastRandom;
import com.gempukku.terasology.procedural.Noise;
import com.gempukku.terasology.procedural.SimplexNoise;
import com.gempukku.terasology.trees.component.IndividualTreeComponent;
import com.gempukku.terasology.trees.component.SimpleTreeDefinitionComponent;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.chunk.WorldGenerator;
import com.gempukku.terasology.world.component.MultiverseComponent;
import com.gempukku.terasology.world.component.SeedComponent;
import com.gempukku.terasology.world.component.WorldComponent;

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = {NetProfiles.AUTHORITY, "hillsWorld"}, shared = WorldGenerator.class)
public class HillsWorldGenerator implements WorldGenerator {
    @In
    private PrefabManager prefabManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;
    @In
    private CommonBlockManager commonBlockManager;

    private float noiseScale = 0.005f;
    private Noise noise = new SimplexNoise(0);

    private short air = -1;
    private short grass = -1;
    private short dirt = -1;
    private short stone = -1;
    private short tree = -1;

    private EntityData oakPrefab;
    private EntityData pinePrefab;

    @Override
    public EntityData createMultiverseEntity() {
        EntityInformation entityInformation = new EntityInformation();
        ComponentInformation multiverse = new ComponentInformation(MultiverseComponent.class);
        multiverse.addField("time", 0L);
        entityInformation.addComponent(multiverse);

        return entityInformation;
    }

    @Override
    public EntityData createWorldEntity(String worldId) {
        EntityInformation entityInformation = new EntityInformation();

        int dayLength = 1 * 60 * 1000;

        ComponentInformation world = new ComponentInformation(WorldComponent.class);
        world.addField("worldId", worldId);
        world.addField("dayLength", dayLength);
        // Start just after sunrise
        world.addField("dayStartDifferenceFromMultiverse", dayLength / 4);
        entityInformation.addComponent(world);

        return entityInformation;
    }

    @Override
    public Iterable<EntityDataOrCommonBlock> generateChunk(String worldId, int x, int y, int z) {
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

        int mountainAmplitude = 32;

        List<EntityDataOrCommonBlock> entities = new LinkedList<>();
        for (int dx = 0; dx < ChunkSize.X; dx++) {
            for (int dy = 0; dy < ChunkSize.Y; dy++) {
                int blockLevel = y * ChunkSize.Y + dy;
                for (int dz = 0; dz < ChunkSize.Z; dz++) {
                    float noiseForColumn = this.noise.noise(noiseScale * (x * ChunkSize.X + dx), noiseScale * (z * ChunkSize.Z + dz));
                    noiseForColumn = (noiseForColumn + 1 / 2);
                    int groundLevel = FastMath.floor(noiseForColumn * mountainAmplitude);
                    if (blockLevel == groundLevel + 1 && dx % (ChunkSize.X / 2) == 0 && dz % (ChunkSize.Z / 2) == 0) {
                        EntityData prefab = (dx == 0) ? oakPrefab : pinePrefab;
                        int maxGenerations = ((Number) prefab.getComponent(SimpleTreeDefinitionComponent.class).getFields().get("maxGenerations")).intValue();
                        EntityInformation entityInformation = new EntityInformation(prefab);

                        ComponentInformation individual = new ComponentInformation(IndividualTreeComponent.class);
                        individual.addField("generation", rnd.nextInt(maxGenerations) + 1);
                        entityInformation.addComponent(individual);

                        ComponentInformation seed = new ComponentInformation(SeedComponent.class);
                        seed.addField("seed", rnd.nextLong());
                        entityInformation.addComponent(seed);

                        entities.add(EntityDataOrCommonBlock.entityData(tree, entityInformation));
                    } else if (blockLevel > groundLevel) {
                        entities.add(EntityDataOrCommonBlock.commonBlock(air));
                    } else if (blockLevel > groundLevel - 1) {
                        entities.add(EntityDataOrCommonBlock.commonBlock(grass));
                    } else if (blockLevel > groundLevel - 3) {
                        entities.add(EntityDataOrCommonBlock.commonBlock(dirt));
                    } else {
                        entities.add(EntityDataOrCommonBlock.commonBlock(stone));
                    }
                }
            }
        }
        return entities;
    }
}
