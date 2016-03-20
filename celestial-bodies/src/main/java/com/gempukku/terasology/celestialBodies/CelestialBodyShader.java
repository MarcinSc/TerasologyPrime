package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;

import java.util.Iterator;

public class CelestialBodyShader extends DefaultShader {
    private static final int NUMBER_OF_BODIES = 10;

    private int celestialBodyDirectionLocation;
    private int celestialBodyColorLocation;
    private int celestialBodySizeLocation;
    private int u_viewportWidth = register("u_viewportWidth");
    private int u_viewportHeight = register("u_viewportHeight");

    private Iterable<CelestialBody> celestialBodies;
    private float viewportWidth;
    private float viewportHeight;

    public CelestialBodyShader(Renderable renderable, Config config) {
        super(renderable, config);
        celestialBodyDirectionLocation = program.fetchUniformLocation("u_celestialBodies[0].positionScreenCoords", false);
        celestialBodyColorLocation = program.fetchUniformLocation("u_celestialBodies[0].color", false);
        celestialBodySizeLocation = program.fetchUniformLocation("u_celestialBodies[0].size", false);
    }

    public void setCelestialBodies(Iterable<CelestialBody> celestialBodies) {
        this.celestialBodies = celestialBodies;
    }

    public void setViewportWidth(float viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public void setViewportHeight(float viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);

        Iterator<CelestialBody> iterator = celestialBodies.iterator();
        for (int i = 0; i < NUMBER_OF_BODIES; i++) {
            if (iterator.hasNext()) {
                CelestialBody celestialBody = iterator.next();
                Vector3 bodyLocation = new Vector3(camera.position);
                bodyLocation.add(new Vector3(celestialBody.directionFromViewpoint).scl(-camera.far));

                float dot = celestialBody.directionFromViewpoint.dot(camera.direction);
                // If the angle between camera and celestial body direction is too large
                // we can hide it. In fact we have to, as projecting the body location
                // also projects it onto a screen directly opposite where it should be.
                if (dot > 0) {
                    Vector3 inScreenCoords = camera.project(bodyLocation);

                    program.setUniformf(celestialBodyDirectionLocation + 2 * i,
                            inScreenCoords.x / camera.viewportWidth,
                            inScreenCoords.y / camera.viewportHeight);
                    program.setUniformf(celestialBodyColorLocation + 3 * i,
                            celestialBody.color.x,
                            celestialBody.color.y,
                            celestialBody.color.z);
                    program.setUniformf(celestialBodySizeLocation + i, celestialBody.cosAngleSize);
                } else {
                    appendEmptyCelestialBody(i);
                }
            } else {
                appendEmptyCelestialBody(i);
            }
        }

        set(u_viewportWidth, viewportWidth);
        set(u_viewportHeight, viewportHeight);
    }

    private void appendEmptyCelestialBody(int i) {
        program.setUniformf(celestialBodyDirectionLocation + 2 * i,
                0, 0);
        program.setUniformf(celestialBodyColorLocation + 3 * i,
                0, 0, 0);
        program.setUniformf(celestialBodySizeLocation + i, 0);
    }

    private float[] multiply(Vector3 vec, float fourth, Matrix4 matrix) {
        float[] l_mat = matrix.getValues();
        return new float[]{
                vec.x * l_mat[Matrix4.M00] + vec.y * l_mat[Matrix4.M01] + vec.z * l_mat[Matrix4.M02] + fourth * l_mat[Matrix4.M03],
                vec.x * l_mat[Matrix4.M10] + vec.y * l_mat[Matrix4.M11] + vec.z * l_mat[Matrix4.M12] + fourth * l_mat[Matrix4.M13],
                vec.x * l_mat[Matrix4.M20] + vec.y * l_mat[Matrix4.M21] + vec.z * l_mat[Matrix4.M22] + fourth * l_mat[Matrix4.M23],
                vec.x * l_mat[Matrix4.M30] + vec.y * l_mat[Matrix4.M31] + vec.z * l_mat[Matrix4.M32] + fourth * l_mat[Matrix4.M33]};
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}