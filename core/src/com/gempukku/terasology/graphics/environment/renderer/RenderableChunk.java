package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.gempukku.terasology.world.chunk.ChunkSize;

public class RenderableChunk implements Disposable {
    private ChunkRenderableBuilder chunkRenderableBuilder;
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    private BoundingBox boundingBox;
    private Model model;
    private ModelInstance modelInstance;

    public RenderableChunk(ChunkRenderableBuilder chunkRenderableBuilder, String worldId, int x, int y, int z) {
        this.chunkRenderableBuilder = chunkRenderableBuilder;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.boundingBox = new BoundingBox(new Vector3(x * ChunkSize.X, y * ChunkSize.Y, z * ChunkSize.Z), new Vector3((x + 1) * ChunkSize.X, (y + 1) * ChunkSize.Y, (z + 1) * ChunkSize.Z));
    }

    public boolean isVisible(Camera camera) {
        return camera.frustum.boundsInFrustum(boundingBox);
    }

    public void updateModel() {
        dispose();

        ChunkRenderableBuilder.BuildResult buildResult = chunkRenderableBuilder.buildChunkRenderable(worldId, x, y, z);
        model = buildResult.model;
        modelInstance = buildResult.modelInstance;
    }

    public RenderableProvider getRenderableProvider() {
        return modelInstance;
    }

    @Override
    public void dispose() {
        if (model != null) {
            model.dispose();
        }
    }
}
