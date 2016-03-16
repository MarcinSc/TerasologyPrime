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
import com.gempukku.terasology.graphics.environment.ChunkMesh;
import com.gempukku.terasology.world.chunk.ChunkSize;

import java.util.List;

public class RenderableChunk {
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
        this.boundingBox = new BoundingBox(new Vector3(x * ChunkSize.X, y * ChunkSize.Y, z * ChunkSize.Z), new Vector3((x + 1) * ChunkSize.X, (y + 1) * ChunkSize.Y, (z + 1) * ChunkSize.Z));
    }

    public boolean isVisible(Camera camera) {
        boolean result = camera.frustum.boundsInFrustum(boundingBox);
        if (result)
            return true;

        // Move camera back one chunk
        camera.position.sub(camera.direction.cpy().scl(ChunkSize.X, ChunkSize.Y, ChunkSize.Z));
        camera.update();

        result = camera.frustum.boundsInFrustum(boundingBox);

        // Move the camera to its original position
        camera.position.add(camera.direction.cpy().scl(ChunkSize.X, ChunkSize.Y, ChunkSize.Z));
        camera.update();

        return result;
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
}
