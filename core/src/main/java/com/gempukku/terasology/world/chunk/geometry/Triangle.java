package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.math.Vector3;

public interface Triangle {
    void getBlockVertices(Vector3 blockLocation,
                          Vector3 vertex1, Vector3 vertex2, Vector3 vertex3,
                          Vector3 normal);
}
