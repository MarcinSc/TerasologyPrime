package com.gempukku.terasology.graphics.postprocess;

import com.badlogic.gdx.graphics.Camera;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.graphics.environment.renderer.RenderingBuffer;

public interface PostProcessingRenderer {
    boolean isEnabled(EntityRef observerEntity);

    void render(EntityRef observerEntity, RenderingBuffer renderingBuffer, Camera camera, int sourceBoundColorTexture, int sourceBoundDepthTexture);
}
