package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.gempukku.terasology.graphics.environment.ChunkMesh;
import com.gempukku.terasology.world.chunk.ChunkSize;

import java.util.List;

public class RenderableChunk implements Disposable {
    public final String worldId;
    public final int x;
    public final int y;
    public final int z;

    private BoundingBox boundingBox;
    private Model model;
    private ModelInstance modelInstance;

    public RenderableChunk(String worldId, int x, int y, int z) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        // Bounding box also spreads to all the chunks around it, because blocks in this chunk can generate a geometry
        // that extends max 1 chunk away
        this.boundingBox = new BoundingBox(new Vector3((x - 1) * ChunkSize.X, (y - 1) * ChunkSize.Y, (z - 1) * ChunkSize.Z), new Vector3((x + 2) * ChunkSize.X, (y + 2) * ChunkSize.Y, (z + 2) * ChunkSize.Z));
    }

    public boolean isVisible(Camera camera) {
        return camera.frustum.boundsInFrustum(boundingBox);
    }

    public void updateChunkMesh(ChunkMesh chunkMesh, List<Texture> textures) {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        for (int i = 0; i < textures.size(); i++) {
            Texture texture = textures.get(i);
            MeshPart meshPart = chunkMesh.getMeshParts().get(i);
            if (meshPart != null) {
                Material material = new Material(TextureAttribute.createDiffuse(texture));
                modelBuilder.part(meshPart, material);
            }
        }

        model = modelBuilder.end();
        modelInstance = new ModelInstance(model);
    }

    public boolean isRenderable() {
        return modelInstance != null;
    }

    public RenderableProvider getRenderableProvider() {
        return modelInstance;
    }

    @Override
    public void dispose() {
        model.dispose();
    }
}
