package com.gempukku.terasology.physics.basic;

import com.badlogic.gdx.math.Vector3;

public interface CharacterShape {
    Iterable<Vector3> getBottomControlPoints();

    float getHeight();

    Iterable<Vector3> getAllControlPoints();
}
