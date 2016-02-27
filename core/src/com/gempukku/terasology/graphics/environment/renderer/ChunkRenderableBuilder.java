package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.HashSet;

public class ChunkRenderableBuilder {
    private BlockMeshGenerator blockMeshGenerator;
    private Collection<Texture> textures;

    public ChunkRenderableBuilder(BlockMeshGenerator blockMeshGenerator, TextureAtlas textureAtlas) {
        this.blockMeshGenerator = blockMeshGenerator;
        this.textures = new HashSet<>();
        Iterables.addAll(textures, textureAtlas.getTextures());
    }

    public BuildResult buildChunkRenderable(String worldId, int x, int y, int z) {
        long start = System.currentTimeMillis();

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        for (Texture texture : textures) {
            Material material = new Material();
            material.set(TextureAttribute.createDiffuse(texture));

            MeshPartBuilder partBuilder = modelBuilder.part("chunk", GL20.GL_TRIANGLES,
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                    material);

            for (int dx = 0; dx < ChunkSize.X; dx++) {
                for (int dy = 0; dy < ChunkSize.Y; dy++) {
                    for (int dz = 0; dz < ChunkSize.Z; dz++) {
                        if (isNotObscuredByOtherBlocksInChunk(dx, dy, dz)) {
                            blockMeshGenerator.generateMeshForBlockFromAtlas(partBuilder, texture, worldId,
                                    x * ChunkSize.X + dx, y * ChunkSize.Y + dy, z * ChunkSize.Z + dz);
                        }
                    }
                }
            }
        }

        for (int dx = 0; dx < ChunkSize.X; dx++) {
            for (int dy = 0; dy < ChunkSize.Y; dy++) {
                for (int dz = 0; dz < ChunkSize.Z; dz++) {
                    if (isNotObscuredByOtherBlocksInChunk(dx, dy, dz)) {
                        blockMeshGenerator.generateCustomMeshForBlock(modelBuilder, worldId,
                                x * ChunkSize.X + dx, y * ChunkSize.Y + dy, z * ChunkSize.Z + dz);
                    }
                }
            }
        }

        Model model = modelBuilder.end();
        ModelInstance modelInstance = new ModelInstance(model);

        System.out.println("Generating mesh for chunk: " + x + "," + y + "," + z + " took " + (System.currentTimeMillis() - start) + "ms.");

        return new BuildResult(model, modelInstance);
    }

    private boolean isNotObscuredByOtherBlocksInChunk(int dx, int dy, int dz) {
        return true;
//        return dx == 0 || dy == 0 || dz == 0
//                || dx == ChunkSize.X - 1 || dy == ChunkSize.Y - 1 || dz == ChunkSize.Z - 1;
    }

    public class BuildResult {
        public final Model model;
        public final ModelInstance modelInstance;

        public BuildResult(Model model, ModelInstance modelInstance) {
            this.model = model;
            this.modelInstance = modelInstance;
        }
    }
}
