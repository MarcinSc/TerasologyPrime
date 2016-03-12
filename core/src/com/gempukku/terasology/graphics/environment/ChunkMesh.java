package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

public class ChunkMesh implements Disposable {
    public class Status {
        private boolean needsOfflineProcessing = true;
        private boolean processingOffline = false;
        private boolean needsMeshUpdate = false;

        public boolean canOfflineProcess() {
            return needsOfflineProcessing && !processingOffline;
        }

        private void setProcessingOffline() {
            processingOffline = true;
            needsOfflineProcessing = false;
        }

        private void setWaitingForModel() {
            processingOffline = false;
            needsMeshUpdate = true;
        }

        private boolean isWaitingForMesh() {
            return needsMeshUpdate;
        }

        private void setHasMesh() {
            needsMeshUpdate = false;
        }

        private void setNeedsOfflineProcessing() {
            needsOfflineProcessing = true;
        }
    }

    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    private volatile Status status;

    private volatile ChunkMeshLists chunkMeshLists;
    private Array<MeshPart> meshParts;

    public ChunkMesh(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        status = new Status();
    }

    public void processOffLine(ChunkMeshGenerator chunkMeshGenerator, List<Texture> textures) {
        synchronized (this) {
            if (!status.canOfflineProcess()) {
                return;
            }
            status.setProcessingOffline();
        }

        ChunkMeshLists generatedLists = chunkMeshGenerator.generateStaticChunkMeshFromChunkBlocks(textures, worldId, x, y, z);
        synchronized (this) {
            chunkMeshLists = generatedLists;
            status.setWaitingForModel();
        }
    }

    public boolean updateModelIfNeeded(ChunkMeshGenerator chunkMeshGenerator) {
        synchronized (this) {
            if (status.isWaitingForMesh()) {
                dispose();

                synchronized (this) {
                    meshParts = chunkMeshGenerator.generateMeshParts(chunkMeshLists);

                    chunkMeshLists = null;
                }
                status.setHasMesh();
                return true;
            }
        }
        return false;
    }

    public void needsUpdate() {
        synchronized (this) {
            status.setNeedsOfflineProcessing();
        }
    }

    public Status getStatus() {
        return status;
    }

    public Array<MeshPart> getMeshParts() {
        return meshParts;
    }

    @Override
    public void dispose() {
        if (meshParts != null) {
            for (MeshPart meshPart : meshParts) {
                if (meshPart != null && meshPart.mesh != null) {
                    meshPart.mesh.dispose();
                }
            }
            meshParts = null;
        }
    }
}
