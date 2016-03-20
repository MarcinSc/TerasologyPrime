package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;

import java.util.Iterator;

public class CelestialBodyShader extends DefaultShader {
    private static final int NUMBER_OF_BODIES = 100;

    private int celestialBodyDirectionLocation;
    private int celestialBodyColorLocation;
    private int celestialBodySizeLocation;
    private int posIncr;
    private int colIncr;
    private int sizeIncr;

    private int u_viewportWidth = register("u_viewportWidth");
    private int u_viewportHeight = register("u_viewportHeight");

    private Iterable<CelestialBody> celestialBodies;
    private float viewportWidth;
    private float viewportHeight;

    public CelestialBodyShader(Renderable renderable, Config config) {
        super(renderable, config);
        celestialBodyDirectionLocation = program.fetchUniformLocation("u_celestialBodies[0].positionScreenCoords", false);
        posIncr = program.fetchUniformLocation("u_celestialBodies[1].positionScreenCoords", false) - celestialBodyDirectionLocation;
        celestialBodyColorLocation = program.fetchUniformLocation("u_celestialBodies[0].color", false);
        colIncr = program.fetchUniformLocation("u_celestialBodies[1].color", false) - celestialBodyColorLocation;
        celestialBodySizeLocation = program.fetchUniformLocation("u_celestialBodies[0].size", false);
        sizeIncr = program.fetchUniformLocation("u_celestialBodies[1].size", false) - celestialBodySizeLocation;
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

                    program.setUniformf(celestialBodyDirectionLocation + posIncr * i,
                            inScreenCoords.x / camera.viewportWidth,
                            inScreenCoords.y / camera.viewportHeight);
                    program.setUniformf(celestialBodyColorLocation + colIncr * i,
                            celestialBody.color.r,
                            celestialBody.color.g,
                            celestialBody.color.b,
                            celestialBody.color.a);
                    program.setUniformf(celestialBodySizeLocation + sizeIncr * i, celestialBody.cosAngleSize);
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
        program.setUniformf(celestialBodyDirectionLocation + posIncr * i,
                0, 0);
        program.setUniformf(celestialBodyColorLocation + colIncr * i,
                0, 0, 0, 0);
        program.setUniformf(celestialBodySizeLocation + sizeIncr * i, 0);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}