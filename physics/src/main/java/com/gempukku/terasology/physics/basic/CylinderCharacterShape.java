package com.gempukku.terasology.physics.basic;

import com.badlogic.gdx.math.Vector3;
import com.gempukku.terasology.physics.component.BasicCylinderPhysicsObjectComponent;

import java.util.LinkedList;
import java.util.List;

public class CylinderCharacterShape implements CharacterShape {
    private BasicCylinderPhysicsObjectComponent cylinderObject;

    public CylinderCharacterShape(BasicCylinderPhysicsObjectComponent cylinderObject) {
        this.cylinderObject = cylinderObject;
    }

    @Override
    public Iterable<Vector3> getBottomControlPoints() {
        float cylinderRadius = cylinderObject.getRadius();

        List<Vector3> result = new LinkedList<>();

        int radiusControlPoints = 8;

        for (int j = 0; j < radiusControlPoints; j++) {
            float dx = cylinderRadius * (float) Math.sin(j * 2 * Math.PI / radiusControlPoints);
            float dz = cylinderRadius * (float) Math.cos(j * 2 * Math.PI / radiusControlPoints);
            result.add(new Vector3(dx, 0, dz));
        }

        result.add(new Vector3(0, 0, 0));

        return result;
    }

    @Override
    public Iterable<Vector3> getAllControlPoints() {
        float cylinderHeight = cylinderObject.getHeight();
        float cylinderRadius = cylinderObject.getRadius();

        List<Vector3> result = new LinkedList<>();

        int heightControlPoints = 3;
        int radiusControlPoints = 8;

        for (int i = 0; i < heightControlPoints; i++) {
            float dy = i * cylinderHeight / (heightControlPoints - 1);
            for (int j = 0; j < radiusControlPoints; j++) {
                float dx = cylinderRadius * (float) Math.sin(j * 2 * Math.PI / radiusControlPoints);
                float dz = cylinderRadius * (float) Math.cos(j * 2 * Math.PI / radiusControlPoints);
                result.add(new Vector3(dx, dy, dz));
            }
        }

        result.add(new Vector3(0, 0, 0));
        result.add(new Vector3(0, cylinderHeight, 0));

        return result;
    }

    @Override
    public float getHeight() {
        return cylinderObject.getHeight();
    }
}
