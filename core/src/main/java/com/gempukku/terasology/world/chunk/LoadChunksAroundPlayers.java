package com.gempukku.terasology.world.chunk;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityManager;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.terasology.world.WorldBlock;
import com.gempukku.terasology.world.component.ClientComponent;
import com.gempukku.terasology.world.component.LocationComponent;

import java.util.LinkedList;
import java.util.List;

@RegisterSystem(
        profiles = {"chunksAroundPlayers", NetProfiles.AUTHORITY})
public class LoadChunksAroundPlayers implements ChunkRelevanceRule, LifeCycleSystem {
    @In
    private ChunkRelevanceRuleRegistry chunkRelevanceRuleRegistry;
    @In
    private EntityManager entityManager;
    @In
    private EntityIndexManager entityIndexManager;

    private int horizontalChunkDistance = 10;
    private int verticalChunkDistance = 3;
    private EntityIndex clientAndLocationIndex;

    private WorldBlock tempBlock = new WorldBlock();

    @Override
    public void initialize() {
        chunkRelevanceRuleRegistry.registerChunkRelevanceRule(this);
        clientAndLocationIndex = entityIndexManager.addIndexOnComponents(ClientComponent.class, LocationComponent.class);
    }

    @Override
    public Iterable<ChunkLocation> getRelevantChunks() {
        List<ChunkLocation> chunkLocationList = new LinkedList<>();

        for (EntityRef player : clientAndLocationIndex.getEntities()) {
            if (player.hasComponent(LocationComponent.class)) {
                LocationComponent location = player.getComponent(LocationComponent.class);
                tempBlock.set(location.getX(), location.getY(), location.getZ());
                String worldId = location.getWorldId();

                for (int x = -horizontalChunkDistance; x <= horizontalChunkDistance; x++) {
                    for (int z = -horizontalChunkDistance; z <= horizontalChunkDistance; z++) {
                        if ((x * x + z * z) <= horizontalChunkDistance * horizontalChunkDistance) {
                            for (int y = -verticalChunkDistance; y <= verticalChunkDistance; y++) {
                                chunkLocationList.add(new ChunkLocationImpl(worldId,
                                        tempBlock.getChunkX() + x,
                                        tempBlock.getChunkY() + y,
                                        tempBlock.getChunkZ() + z));
                            }
                        }
                    }
                }
            }
        }

        return chunkLocationList;
    }

    @Override
    public boolean isChunkRelevant(ChunkLocation chunk) {
        for (EntityRef player : clientAndLocationIndex.getEntities()) {
            if (player.hasComponent(LocationComponent.class)) {
                LocationComponent location = player.getComponent(LocationComponent.class);
                tempBlock.set(location.getX(), location.getY(), location.getZ());
                String worldId = location.getWorldId();

                if (worldId.equals(chunk.getWorldId())) {
                    int diffX = Math.abs(chunk.getX() - tempBlock.getChunkX());
                    int diffY = Math.abs(chunk.getY() - tempBlock.getChunkY());
                    int diffZ = Math.abs(chunk.getZ() - tempBlock.getChunkZ());

                    if (diffX <= horizontalChunkDistance && diffY <= verticalChunkDistance && diffZ <= horizontalChunkDistance)
                        return true;
                }
            }
        }
        return false;
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
