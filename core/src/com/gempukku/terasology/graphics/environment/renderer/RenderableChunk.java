package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.gempukku.terasology.world.chunk.ChunkSize;

public class RenderableChunk implements Disposable {
    public class Status {
        private boolean needsOfflineProcessing = false;
        private boolean processingOffline = false;
        private boolean needsModelUpdate = false;
        private boolean invalid = true;

        public boolean canOfflineProcess() {
            return needsOfflineProcessing && !processingOffline && !invalid;
        }

        private void setProcessingOffline() {
            processingOffline = true;
            needsOfflineProcessing = false;
        }

        private void setWaitingForModel() {
            processingOffline = false;
            needsModelUpdate = true;
        }

        private boolean isWaitingForModel() {
            return needsModelUpdate && !invalid;
        }

        private void setHasModel() {
            needsModelUpdate = false;
        }

        private void setNeedsOfflineProcessing() {
            needsOfflineProcessing = true;
        }

        private void invalidate() {
            invalid = true;
        }

        private void validate() {
            invalid = false;
            needsModelUpdate = false;
            needsOfflineProcessing = true;
        }

        public boolean isInvalid() {
            return invalid;
        }
    }

    private ChunkRenderableBuilder chunkRenderableBuilder;
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    private BoundingBox boundingBox;
    private Model model;
    private ModelInstance modelInstance;

    private volatile Status status;

    private volatile ChunkMeshLists chunkMeshLists;

    public RenderableChunk(ChunkRenderableBuilder chunkRenderableBuilder, String worldId, int x, int y, int z) {
        this.chunkRenderableBuilder = chunkRenderableBuilder;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.boundingBox = new BoundingBox(new Vector3(x * ChunkSize.X, y * ChunkSize.Y, z * ChunkSize.Z), new Vector3((x + 1) * ChunkSize.X, (y + 1) * ChunkSize.Y, (z + 1) * ChunkSize.Z));
        status = new Status();
    }

    public boolean isVisible(Camera camera) {
        return camera.frustum.boundsInFrustum(boundingBox);
    }

    public void processOffLine() {
        synchronized (this) {
            if (!status.canOfflineProcess()) {
                return;
            }
            status.setProcessingOffline();
        }

        ChunkMeshLists generatedLists = chunkRenderableBuilder.buildChunkMeshArrays(worldId, x, y, z);
        synchronized (this) {
            chunkMeshLists = generatedLists;
            status.setWaitingForModel();
        }
    }

    public void updateModelIfNeeded() {
        synchronized (this) {
            if (status.isWaitingForModel()) {
                dispose();

                synchronized (this) {
                    model = chunkRenderableBuilder.buildChunkRenderable(chunkMeshLists.verticesPerTexture, chunkMeshLists.indicesPerTexture, worldId, x, y, z);

                    modelInstance = new ModelInstance(model);

                    chunkMeshLists = null;
                }
                status.setHasModel();
            }
        }
    }

    public void needsUpdate() {
        synchronized (this) {
            status.setNeedsOfflineProcessing();
        }
    }

    public void invalidateChunk() {
        synchronized (this) {
            status.invalidate();
            model.dispose();
            model = null;
        }
    }

    public void validate() {
        synchronized (this) {
            status.validate();
        }
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRenderable() {
        return modelInstance != null;
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
