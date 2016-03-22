package com.gempukku.terasology.celestialBodies.model;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public class CircleCelestialBody implements CelestialBody {
    private int rendererIndex;
    private final Color color;
    private final Vector3 directionFromViewpoint;
    private final float bodySizeInDeviceCoords;

    public CircleCelestialBody(int rendererIndex, Color color, Vector3 directionFromViewpoint, float bodySizeInDeviceCoords) {
        this.rendererIndex = rendererIndex;
        this.color = color;
        this.directionFromViewpoint = directionFromViewpoint;
        this.bodySizeInDeviceCoords = bodySizeInDeviceCoords;
    }

    @Override
    public int getDataFloatCount() {
        return 7;
    }

    @Override
    public boolean isVisibleFrom(Camera camera) {
        Vector3 bodyLocation = new Vector3(camera.position);
        bodyLocation.add(new Vector3(directionFromViewpoint).scl(camera.far));

        return camera.frustum.sphereInFrustum(new Vector3(bodyLocation).scl(0.99f), 10f);
    }

    @Override
    public void appendFloats(float[] array, int startIndex, Camera camera) {
        Vector3 bodyLocation = new Vector3(camera.position);
        bodyLocation.add(new Vector3(directionFromViewpoint).scl(camera.far));
        Vector3 inScreenCoords = camera.project(bodyLocation);

        array[startIndex] = rendererIndex;
        array[startIndex + 1] = inScreenCoords.x / camera.viewportWidth;
        array[startIndex + 2] = inScreenCoords.y / camera.viewportHeight;
        array[startIndex + 3] = color.r;
        array[startIndex + 4] = color.g;
        array[startIndex + 5] = color.b;
        array[startIndex + 6] = color.a;
        array[startIndex + 7] = bodySizeInDeviceCoords;
    }
}
