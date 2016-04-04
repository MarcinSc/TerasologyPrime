package com.gempukku.terasology.landd.system;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.landd.component.PermanentChunkLoadingComponent;
import com.gempukku.terasology.world.MultiverseManager;
import com.gempukku.terasology.world.chunk.ChunkLocation;
import com.gempukku.terasology.world.chunk.ChunkRelevanceRule;
import com.gempukku.terasology.world.chunk.ChunkRelevanceRuleRegistry;

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = NetProfiles.AUTHORITY)
public class LoadPredefinedChunksRelevancy implements ChunkRelevanceRule, LifeCycleSystem {
    @In
    private MultiverseManager multiverseManager;
    @In
    private ChunkRelevanceRuleRegistry chunkRelevanceRuleRegistry;

    private List<ChunkLocation> relevantLocations;

    @Override
    public void initialize() {
        chunkRelevanceRuleRegistry.registerChunkRelevanceRule(this);
    }

    public void init() {
        if (relevantLocations == null) {
            EntityRef multiverseEntity = multiverseManager.getMultiverseEntity();

            relevantLocations = new LinkedList<>();
            PermanentChunkLoadingComponent chunkLoading = multiverseEntity.getComponent(PermanentChunkLoadingComponent.class);
            String worldId = chunkLoading.getWorldId();
            int minX = chunkLoading.getMinimumChunkX();
            int minY = chunkLoading.getMinimumChunkY();
            int minZ = chunkLoading.getMinimumChunkZ();
            int maxX = chunkLoading.getMaximumChunkX();
            int maxY = chunkLoading.getMaximumChunkY();
            int maxZ = chunkLoading.getMaximumChunkZ();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        relevantLocations.add(
                                new ChunkLocationImpl(worldId, x, y, z));
                    }
                }
            }
        }
    }

    @Override
    public Iterable<ChunkLocation> getRelevantChunks() {
        init();
        return relevantLocations;
    }

    @Override
    public boolean isChunkRelevant(ChunkLocation chunk) {
        EntityRef multiverseEntity = multiverseManager.getMultiverseEntity();
        PermanentChunkLoadingComponent chunkLoading = multiverseEntity.getComponent(PermanentChunkLoadingComponent.class);
        return chunkLoading.getWorldId().equals(chunk.getWorldId())
                && chunk.getX() >= chunkLoading.getMinimumChunkX()
                && chunk.getY() >= chunkLoading.getMinimumChunkY()
                && chunk.getZ() >= chunkLoading.getMinimumChunkZ()
                && chunk.getX() <= chunkLoading.getMaximumChunkX()
                && chunk.getY() <= chunkLoading.getMaximumChunkY()
                && chunk.getZ() <= chunkLoading.getMaximumChunkZ();
    }

    private static class ChunkLocationImpl implements ChunkLocation {
        private String worldId;
        private int x;
        private int y;
        private int z;

        public ChunkLocationImpl(String worldId, int x, int y, int z) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String getWorldId() {
            return worldId;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }
    }
}
