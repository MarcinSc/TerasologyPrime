package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkSize;
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

    public Model buildChunkRenderable(String worldId, int x, int y, int z) {
        long start = System.currentTimeMillis();

        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, x, y, z);

        blockMeshGenerator.generateChunkMeshFromChunkBlocks(modelBuilder, chunkBlocks, textures);

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
}
