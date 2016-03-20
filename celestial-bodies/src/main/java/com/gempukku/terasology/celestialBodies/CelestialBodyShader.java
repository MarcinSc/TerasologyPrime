package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;

import java.util.Iterator;

public class CelestialBodyShader extends DefaultShader {
    private static final int NUMBER_OF_BODIES = 70;

    private int celestialBodyParamsLocation;

    private int u_viewportWidth = register("u_viewportWidth");
    private int u_viewportHeight = register("u_viewportHeight");

    private Iterable<CelestialBody> celestialBodies;
    private float viewportWidth;
    private float viewportHeight;

    private float[] celestialBodyParams = new float[490];

    public CelestialBodyShader(Renderable renderable, Config config) {
        super(renderable, config);
        celestialBodyParamsLocation = program.fetchUniformLocation("u_celestialBodiesParams[0]", false);
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
                bodyLocation.add(new Vector3(celestialBody.directionFromViewpoint).scl(camera.far));

                // If the angle between camera and celestial body direction is too large
                // we can hide it. In fact we have to, as projecting the body location
                // also projects it onto a screen directly opposite where it should be.
                if (camera.frustum.sphereInFrustum(new Vector3(bodyLocation).scl(0.99f), 10f)) {
                    Vector3 inScreenCoords = camera.project(bodyLocation);
                    celestialBodyParams[7 * i] = inScreenCoords.x / camera.viewportWidth;
                    celestialBodyParams[7 * i + 1] = inScreenCoords.y / camera.viewportHeight;
                    celestialBodyParams[7 * i + 2] = celestialBody.color.r;
                    celestialBodyParams[7 * i + 3] = celestialBody.color.g;
                    celestialBodyParams[7 * i + 4] = celestialBody.color.b;
                    celestialBodyParams[7 * i + 5] = celestialBody.color.a;
                    celestialBodyParams[7 * i + 6] = celestialBody.cosAngleSize;
                } else {
                    appendEmptyCelestialBody(i);
                }
            } else {
                appendEmptyCelestialBody(i);
            }
        }

        program.setUniform1fv(celestialBodyParamsLocation, celestialBodyParams, 0, celestialBodyParams.length);

        set(u_viewportWidth, viewportWidth);
        set(u_viewportHeight, viewportHeight);
    }

    private void appendEmptyCelestialBody(int i) {
        celestialBodyParams[7 * i] = 0;
        celestialBodyParams[7 * i + 1] = 0;
        celestialBodyParams[7 * i + 2] = 0;
        celestialBodyParams[7 * i + 3] = 0;
        celestialBodyParams[7 * i + 4] = 0;
        celestialBodyParams[7 * i + 5] = 0;
        celestialBodyParams[7 * i + 6] = 0;
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}