package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.procedural.Noise;
import com.gempukku.terasology.procedural.SimplexNoise;
import com.gempukku.terasology.world.CommonBlockManager;

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = {NetProfiles.AUTHORITY, "hillsWorld"}, shared = ChunkGenerator.class)
public class HillsWorldChunkGenerator implements ChunkGenerator {
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

    @Override
    public Iterable<EntityDataOrCommonBlock> generateChunk(String worldId, int x, int y, int z) {
        if (air == -1) {
            air = commonBlockManager.getCommonBlockId("air");
            grass = commonBlockManager.getCommonBlockId("grass");
            dirt = commonBlockManager.getCommonBlockId("dirt");
            stone = commonBlockManager.getCommonBlockId("stone");
            tree = commonBlockManager.getCommonBlockId("tree");
        }

        int mountainAmplitude = 32;

        List<EntityDataOrCommonBlock> entities = new LinkedList<>();
        for (int dx = 0; dx < ChunkSize.X; dx++) {
            for (int dy = 0; dy < ChunkSize.Y; dy++) {
                int blockLevel = y * ChunkSize.Y + dy;
                for (int dz = 0; dz < ChunkSize.Z; dz++) {
                    float noiseForColumn = this.noise.noise(noiseScale * (x * ChunkSize.X + dx), noiseScale * (z * ChunkSize.Z + dz));
                    noiseForColumn = (noiseForColumn + 1 / 2);
                    int groundLevel = FastMath.floor(noiseForColumn * mountainAmplitude);
                    if (blockLevel == groundLevel + 1 && dx == 0 && dz == 0) {
                        entities.add(EntityDataOrCommonBlock.commonBlock(tree));
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
