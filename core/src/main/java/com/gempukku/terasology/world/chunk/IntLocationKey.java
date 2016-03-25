package com.gempukku.terasology.world.chunk;

public class IntLocationKey {
    private final String worldId;
    private final int x;
    private final int y;
    private final int z;

    public IntLocationKey(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public IntLocationKey(ChunkLocation chunkLocation) {
        this(chunkLocation.getWorldId(), chunkLocation.getX(), chunkLocation.getY(), chunkLocation.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntLocationKey that = (IntLocationKey) o;

        if (x != that.x) return false;
        if (y != that.y) return false;
        if (z != that.z) return false;
        return worldId.equals(that.worldId);
    }

    @Override
    public int hashCode() {
        int result = worldId.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }
}
