package com.gempukku.terasology.celestialBodies.model;

import com.badlogic.gdx.math.Vector3;

public class CelestialBody {
    public final Vector3 color;
    public final Vector3 directionFromViewpoint;
    public final float cosAngleSize;

    public CelestialBody(Vector3 color, Vector3 directionFromViewpoint, float cosAngleSize) {
        this.color = color;
        this.directionFromViewpoint = directionFromViewpoint;
        this.cosAngleSize = cosAngleSize;
    }
}
