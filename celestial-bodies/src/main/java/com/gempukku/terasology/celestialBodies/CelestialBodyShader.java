package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;

import java.util.Iterator;

public class CelestialBodyShader extends DefaultShader {
    private static final int BODY_ARRAY_SIZE = 500;

    private int celestialBodyParamsLocation;

    private int u_viewportWidth = register("u_viewportWidth");
    private int u_viewportHeight = register("u_viewportHeight");

    private Iterator<CelestialBody> celestialBodies;
    private CelestialBody nextBody;

    private float[] celestialBodyParams = new float[BODY_ARRAY_SIZE];

    public CelestialBodyShader(Renderable renderable, Config config) {
        super(renderable, config);
        celestialBodyParamsLocation = program.fetchUniformLocation("u_celestialBodiesParams[0]", false);
    }

    public void prepareCelestialBodies(Iterable<CelestialBody> celestialBodies) {
        this.celestialBodies = celestialBodies.iterator();
        if (this.celestialBodies.hasNext()) {
            nextBody = this.celestialBodies.next();
        } else {
            nextBody = null;
        }
    }

    public boolean hasBodiesToRender() {
        return nextBody != null;
    }

    @Override
    public void begin(Camera camera, RenderContext context) {
        super.begin(camera, context);

        int arrayIndex = 1;
        int celestialBodyCount = 0;
        while (nextBody != null) {
            if (nextBody.isVisibleFrom(camera)) {
                int floatCount = nextBody.getDataFloatCount();
                // Check if we have space in array for it
                if (arrayIndex + 1 + floatCount < BODY_ARRAY_SIZE) {
                    nextBody.appendFloats(celestialBodyParams, arrayIndex, camera);

                    arrayIndex += 1 + floatCount;
                    celestialBodyCount++;
                } else {
                    break;
                }
            }
            if (celestialBodies.hasNext())
                nextBody = celestialBodies.next();
            else
                nextBody = null;
        }
        celestialBodyParams[0] = celestialBodyCount;

        program.setUniform1fv(celestialBodyParamsLocation, celestialBodyParams, 0, celestialBodyParams.length);

        set(u_viewportWidth, camera.viewportWidth);
        set(u_viewportHeight, camera.viewportHeight);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}