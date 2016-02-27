package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Camera;

public interface EnvironmentRenderer {
    void renderEnvironment(Camera camera, String worldId);
}
