package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.procedural.FastMath;
import com.gempukku.terasology.procedural.Noise;
import com.gempukku.terasology.procedural.SimplexNoise;

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = "hillsWorld", shared = ChunkGenerator.class)
public class HillsWorldChunkGenerator implements ChunkGenerator {
    @In
    private PrefabManager prefabManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;

    private float noiseScale = 0.005f;
    private Noise noise = new SimplexNoise(0);

    @Override
    public Iterable<EntityDataOrCommonBlock> generateChunk(String worldId, int x, int y, int z) {
        int mountainAmplitude = 32;

        List<EntityDataOrCommonBlock> entities = new LinkedList<>();
        for (int dx = 0; dx < ChunkSize.X; dx++) {
            for (int dy = 0; dy < ChunkSize.Y; dy++) {
                int blockLevel = y * ChunkSize.Y + dy;
                for (int dz = 0; dz < ChunkSize.Z; dz++) {
                    float noiseForColumn = this.noise.noise(noiseScale * (x * ChunkSize.X + dx), noiseScale * (z * ChunkSize.Z + dz));
                    noiseForColumn = (noiseForColumn + 1 / 2);
                    int groundLevel = FastMath.floor(noiseForColumn * mountainAmplitude);
                    if (blockLevel > groundLevel) {
                        entities.add(EntityDataOrCommonBlock.commonBlock("air"));
                    } else if (blockLevel > groundLevel - 1) {
                        entities.add(EntityDataOrCommonBlock.commonBlock("grass"));
                    } else if (blockLevel > groundLevel - 3) {
                        entities.add(EntityDataOrCommonBlock.commonBlock("dirt"));
                    } else {
                        entities.add(EntityDataOrCommonBlock.commonBlock("stone"));
                    }
                }
            }
        }
        return entities;
    }
}
