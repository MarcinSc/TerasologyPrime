package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

public interface EnvironmentRenderer {
    void renderEnvironment(Camera camera, String worldId, ModelBatch modelBatch);
}
