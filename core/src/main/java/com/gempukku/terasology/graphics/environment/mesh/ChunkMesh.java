package com.gempukku.terasology.graphics.environment.mesh;

import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
import com.gempukku.terasology.world.chunk.ChunkLocation;

public class ChunkMesh implements ChunkLocation {
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    private Array<MeshPart> meshParts;

    public ChunkMesh(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Array<MeshPart> getMeshParts() {
        return meshParts;
    }

    public void setMeshParts(Array<MeshPart> meshParts) {
        this.meshParts = meshParts;
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
