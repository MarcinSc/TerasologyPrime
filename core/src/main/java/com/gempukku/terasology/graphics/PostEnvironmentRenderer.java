package com.gempukku.terasology.graphics;

import com.badlogic.gdx.graphics.Camera;

public interface PostEnvironmentRenderer {
    void renderPostEnvironment(Camera camera, String worldId);
}
