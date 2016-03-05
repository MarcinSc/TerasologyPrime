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
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;

public class ChunkRenderableBuilder {
    private BlockMeshGenerator blockMeshGenerator;
    private ChunkBlocksProvider chunkBlocksProvider;
    private List<Texture> textures;

    public ChunkRenderableBuilder(BlockMeshGenerator blockMeshGenerator, ChunkBlocksProvider chunkBlocksProvider, TextureAtlas textureAtlas) {
        this.blockMeshGenerator = blockMeshGenerator;
        this.chunkBlocksProvider = chunkBlocksProvider;
        this.textures = new ArrayList<>();
        Iterables.addAll(textures, textureAtlas.getTextures());
    }

    public ChunkMeshLists buildChunkMeshArrays(String worldId, int x, int y, int z) {
        ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, x, y, z);

        // Blocks might be removed as we generate stuff
        if (chunkBlocks == null)
            return null;

        return blockMeshGenerator.generateChunkMeshFromChunkBlocks(chunkBlocks, textures);
    }

    public Model buildChunkRenderable(List<float[]> verticesPerTexture, List<short[]> indicesPerTexture, String worldId, int x, int y, int z) {
        long start = System.currentTimeMillis();

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        int textureCount = textures.size();
        for (int i = 0; i < textureCount; i++) {
            float[] vertices = verticesPerTexture.get(i);
            short[] indices = indicesPerTexture.get(i);

            if (indices.length > 0) {
                int parameterCount = 3 + 3 + 2;

                Mesh mesh = new Mesh(true, vertices.length / parameterCount, indices.length, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
                mesh.setVertices(vertices);
                mesh.setIndices(indices);

                Texture texture = textures.get(i);
                Material material = new Material(TextureAttribute.createDiffuse(texture));

                modelBuilder.part("chunk", mesh, GL20.GL_TRIANGLES, material);
            }
        }

        Model model = modelBuilder.end();

//        System.out.println("Generating mesh for chunk: " + x + "," + y + "," + z + " took " + (System.currentTimeMillis() - start) + "ms.");

        return model;
    }
}
