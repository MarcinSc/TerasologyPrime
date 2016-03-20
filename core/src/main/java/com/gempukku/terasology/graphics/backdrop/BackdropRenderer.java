package com.gempukku.terasology.graphics.backdrop;

import com.badlogic.gdx.graphics.Camera;

public interface BackdropRenderer {
    void renderBackdrop(Camera camera, String worldId);
}
