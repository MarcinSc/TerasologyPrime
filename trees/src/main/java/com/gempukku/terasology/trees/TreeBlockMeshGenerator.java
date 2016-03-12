package com.gempukku.terasology.trees;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.graphics.environment.BlockMeshGenerator;
import com.gempukku.terasology.graphics.environment.BlockMeshGeneratorRegistry;
import com.gempukku.terasology.graphics.environment.ChunkMeshGeneratorCallback;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.chunk.ChunkBlocks;

import java.util.Arrays;

@RegisterSystem(
        profiles = "generateChunkMeshes")
public class TreeBlockMeshGenerator implements BlockMeshGenerator, LifeCycleSystem {
    @In
    private BlockMeshGeneratorRegistry blockMeshGeneratorRegistry;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private ShapeProvider shapeProvider;

    private TextureRegion oakBarkTexture;
    private TextureRegion oakLeafTexture;
    private ShapeDef cubeShape;

    @Override
    public void initialize() {
        textureAtlasRegistry.registerTextures(
                Arrays.asList(
                        "blockTiles/plant/Tree/OakBark.png",
                        "blockTiles/plant/leaf/GreenLeaf.png"));
        blockMeshGeneratorRegistry.registerBlockMeshGenerator("trees:tree", this);
        cubeShape = shapeProvider.getShapeById("cube");
    }

    @Override
    public void generateMeshForBlockFromAtlas(ChunkMeshGeneratorCallback callback, FloatArray vertices, ShortArray indices,
                                              Texture texture, ChunkBlocks chunkBlocks,
                                              int chunkX, int chunkY, int chunkZ, int x, int y, int z) {
        init();

        if (texture == oakBarkTexture.getTexture()) {
            // 5 segments of bark - 1.8 meters tall each
            float segmentHeight = 1.8f;

            float[] segmentSkewX = new float[]{0f, 0.15f, -0.1f, 0.12f, 0.18f, 0f};
            float[] segmentSkewZ = new float[]{0f, 0.09f, -0.1f, 0.08f, 0.1f, -0.08f};

            for (int i = 0; i < 5; i++) {
                float trunkDiameterStart = (5 - i) * 0.1f;
                float trunkDiameterEnd = (5 - i - 1) * 0.1f;
                float leavesDiameterStart = (i < 2) ? 0 : (5 - i) * 0.8f;
                float leavesDiameterEnd = (i < 1) ? 0 : (5 - i - 1) * 0.8f;

                float trunkStartY = i * segmentHeight;
                float trunkEndY = (i + 1) * segmentHeight;

                float bottomXSkew = 0.5f + segmentSkewX[i];
                float bottomZSkew = 0.5f + segmentSkewZ[i];
                float topXSkew = 0.5f + segmentSkewX[i + 1];
                float topZSkew = 0.5f + segmentSkewZ[i + 1];

                for (ShapePartDef shapePart : cubeShape.getShapeParts()) {
                    String side = shapePart.getSide();
                    if (!side.equals("top") && !side.equals("bottom")) {
                        int vertexCount = shapePart.getVertices().size();

                        // This array will store indexes of vertices in the resulting Mesh
                        short[] vertexMapping = new short[vertexCount];

                        // Trunk
                        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
                            vertexMapping[vertexIndex] = (short) (vertices.size / 8);

                            Float[] vertexCoords = shapePart.getVertices().get(vertexIndex);
                            Float[] normalValues = shapePart.getNormals().get(vertexIndex);
                            Float[] textureCoords = shapePart.getUvs().get(vertexIndex);

                            boolean bottom = vertexCoords[1] == 0;
                            float xSkew = bottom ? bottomXSkew : topXSkew;
                            float zSkew = bottom ? bottomZSkew : topZSkew;

                            vertices.add(chunkX + x + xSkew + (vertexCoords[0] - 0.5f) * (bottom ? trunkDiameterStart : trunkDiameterEnd));
                            vertices.add(chunkY + y + trunkStartY + (vertexCoords[1] * (trunkEndY - trunkStartY)));
                            vertices.add(chunkZ + z + zSkew + (vertexCoords[2] - 0.5f) * (bottom ? trunkDiameterStart : trunkDiameterEnd));
                            vertices.add(normalValues[0]);
                            vertices.add(normalValues[1]);
                            vertices.add(normalValues[2]);
                            vertices.add(oakBarkTexture.getU() + textureCoords[0] * (oakBarkTexture.getU2() - oakBarkTexture.getU()));
                            vertices.add(oakBarkTexture.getV() + textureCoords[1] * (oakBarkTexture.getV2() - oakBarkTexture.getV()));
                        }
                        for (short index : shapePart.getIndices()) {
                            indices.add(vertexMapping[index]);
                        }

                        // Leaves
                        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
                            vertexMapping[vertexIndex] = (short) (vertices.size / 8);

                            Float[] vertexCoords = shapePart.getVertices().get(vertexIndex);
                            Float[] normalValues = shapePart.getNormals().get(vertexIndex);
                            Float[] textureCoords = shapePart.getUvs().get(vertexIndex);

                            boolean bottom = vertexCoords[1] == 0;
                            float xSkew = bottom ? bottomXSkew : topXSkew;
                            float zSkew = bottom ? bottomZSkew : topZSkew;

                            vertices.add(chunkX + x + xSkew + (vertexCoords[0] - 0.5f) * (bottom ? leavesDiameterStart : leavesDiameterEnd));
                            vertices.add(chunkY + y + trunkStartY + (vertexCoords[1] * (trunkEndY - trunkStartY)));
                            vertices.add(chunkZ + z + zSkew + (vertexCoords[2] - 0.5f) * (bottom ? leavesDiameterStart : leavesDiameterEnd));
                            vertices.add(normalValues[0]);
                            vertices.add(normalValues[1]);
                            vertices.add(normalValues[2]);
                            vertices.add(oakLeafTexture.getU() + textureCoords[0] * (oakLeafTexture.getU2() - oakLeafTexture.getU()));
                            vertices.add(oakLeafTexture.getV() + textureCoords[1] * (oakLeafTexture.getV2() - oakLeafTexture.getV()));
                        }
                        for (short index : shapePart.getIndices()) {
                            indices.add(vertexMapping[index]);
                        }
                    }
                }
            }
        }
    }

    private void init() {
        if (oakBarkTexture == null) {
            oakBarkTexture = textureAtlasProvider.getTexture("blockTiles/plant/Tree/OakBark");
            oakLeafTexture = textureAtlasProvider.getTexture("blockTiles/plant/leaf/GreenLeaf");
        }
    }
}
