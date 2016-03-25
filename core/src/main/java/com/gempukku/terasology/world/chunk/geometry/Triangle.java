package com.gempukku.terasology.world.chunk.geometry;

import com.badlogic.gdx.math.Vector3;

public interface Triangle {
    Vector3 intersectLineSegment(float x1, float y1, float z1, float x2, float y2, float z2, Vector3 result);
}
