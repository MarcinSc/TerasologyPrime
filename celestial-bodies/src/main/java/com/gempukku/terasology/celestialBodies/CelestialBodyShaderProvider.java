package com.gempukku.terasology.celestialBodies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.utils.StringBuilder;
import com.gempukku.terasology.celestialBodies.model.CelestialBody;

import java.util.List;

public class CelestialBodyShaderProvider implements ShaderProvider {
    private CelestialBodyShader celestialBodyShader;

    private String functionReplacement;
    private String functionDispatch;

    public CelestialBodyShaderProvider(List<CelestialBodyTypeRenderer> renderers) {
        StringBuilder functions = new StringBuilder();
        StringBuilder dispatch = new StringBuilder();
        for (int i = 0; i < renderers.size(); i++) {
            functions.append("vec4 getPixelColor" + i + "(vec2 fragmentScreenCoords, float aspectRatio, int arrayIndex) {\n" +
                    renderers.get(i).getShaderSnippet() + "\n}\n\n");
            if (i != 0) {
                dispatch.append(" else ");
            }
            dispatch.append("if (bodyType == " + i + ".0) {\n" +
                    "result = getPixelColor" + i + "(fragmentScreenCoords, aspectRatio, arrayIndex);\n" +
                    "arrayIndex += " + renderers.get(i).getDataFloatCount() + ";\n" +
                    "}");
        }

        functionReplacement = functions.toString();
        functionDispatch = dispatch.toString();
    }

    public void prepareCelestialBodies(Iterable<CelestialBody> celestialBodies) {
        if (celestialBodyShader != null) {
            celestialBodyShader.prepareCelestialBodies(celestialBodies);
        }
    }

    public boolean hasBodiesToRender() {
        return celestialBodyShader == null || celestialBodyShader.hasBodiesToRender();
    }

    @Override
    public Shader getShader(Renderable renderable) {
        if (celestialBodyShader == null) {
            String fragmentShader = Gdx.files.internal("shader/celestial.frag.template").readString()
                    .replace("${functions}", functionReplacement).replace("${dispatch}", functionDispatch);
            DefaultShader.Config config = new DefaultShader.Config(
                    Gdx.files.internal("shader/celestial.vert").readString(),
                    fragmentShader);
            config.defaultDepthFunc = 0;
            celestialBodyShader = new CelestialBodyShader(renderable,
                    config);
            celestialBodyShader.init();
        }
        return celestialBodyShader;
    }

    @Override
    public void dispose() {
        if (celestialBodyShader != null)
            celestialBodyShader.dispose();
    }
}
