package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ChunkRenderableBuilder {
    private BlockMeshGenerator blockMeshGenerator;
    private Collection<Texture> textures;

    public ChunkRenderableBuilder(BlockMeshGenerator blockMeshGenerator, TextureAtlas textureAtlas) {
        this.blockMeshGenerator = blockMeshGenerator;
        this.textures = new HashSet<>();
        Iterables.addAll(textures, textureAtlas.getTextures());
    }

    public Model buildChunkRenderable(String worldId, int x, int y, int z) {
        long start = System.currentTimeMillis();

        int parameterCount = 3 + 3 + 2;

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        long startArrays = System.currentTimeMillis();
        for (Texture texture : textures) {
            List<Float> vertices = new LinkedList<>();
            List<Short> indices = new LinkedList<>();

            for (int dx = 0; dx < ChunkSize.X; dx++) {
                for (int dy = 0; dy < ChunkSize.Y; dy++) {
                    for (int dz = 0; dz < ChunkSize.Z; dz++) {
                        blockMeshGenerator.generateMeshForBlockFromAtlas(vertices, indices, texture, worldId,
                                x * ChunkSize.X + dx, y * ChunkSize.Y + dy, z * ChunkSize.Z + dz);
                    }
                }
            }

            if (indices.size() > 0) {
                Mesh mesh = new Mesh(true, vertices.size() / parameterCount, indices.size(), VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
                mesh.setVertices(convertToFloatArray(vertices));
                mesh.setIndices(convertToShortArray(indices));

                Material material = new Material(TextureAttribute.createDiffuse(texture));

                modelBuilder.part("chunk", mesh, GL20.GL_TRIANGLES, material);
            }
        }

        System.out.println("Creating arrays took: " + (System.currentTimeMillis() - startArrays) + "ms.");

        for (int dx = 0; dx < ChunkSize.X; dx++) {
            for (int dy = 0; dy < ChunkSize.Y; dy++) {
                for (int dz = 0; dz < ChunkSize.Z; dz++) {
                    blockMeshGenerator.generateCustomMeshForBlock(modelBuilder, worldId,
                            x * ChunkSize.X + dx, y * ChunkSize.Y + dy, z * ChunkSize.Z + dz);
                }
            }
        }

        Model model = modelBuilder.end();

        System.out.println("Generating mesh for chunk: " + x + "," + y + "," + z + " took " + (System.currentTimeMillis() - start) + "ms.");

        return model;
    }

    private float[] convertToFloatArray(Collection<Float> elements) {
        float[] result = new float[elements.size()];
        Iterator<Float> iterator = elements.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = iterator.next();
        }
        return result;
    }

    private short[] convertToShortArray(Collection<Short> elements) {
        short[] result = new short[elements.size()];
        Iterator<Short> iterator = elements.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = iterator.next();
        }
        return result;
    }
}
