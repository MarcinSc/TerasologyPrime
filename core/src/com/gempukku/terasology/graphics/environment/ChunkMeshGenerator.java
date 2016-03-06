package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.component.ShapeAndTextureComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChunkMeshGenerator {
    private ChunkBlocksProvider chunkBlocksProvider;
    private final CommonBlockManager commonBlockManager;
    private final TextureAtlasProvider textureAtlasProvider;
    private final ShapeProvider shapeProvider;

    private String shapeAndTextureComponentName;

    public ChunkMeshGenerator(ChunkBlocksProvider chunkBlocksProvider, CommonBlockManager commonBlockManager, TextureAtlasProvider textureAtlasProvider,
                              TerasologyComponentManager terasologyComponentManager, ShapeProvider shapeProvider) {
        this.chunkBlocksProvider = chunkBlocksProvider;
        this.commonBlockManager = commonBlockManager;
        this.textureAtlasProvider = textureAtlasProvider;
        this.shapeProvider = shapeProvider;

        shapeAndTextureComponentName = terasologyComponentManager.getNameByComponent(ShapeAndTextureComponent.class);
    }

    // Would be nice to get rid of the "textures" here
    public ChunkMeshLists generateStaticChunkMeshFromChunkBlocks(List<Texture> textures, String worldId, int x, int y, int z) {
        ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, x, y, z);
        int chunkX = chunkBlocks.x * ChunkSize.X;
        int chunkY = chunkBlocks.y * ChunkSize.Y;
        int chunkZ = chunkBlocks.z * ChunkSize.Z;

        int textureCount = textures.size();

        List<float[]> verticesPerTexture = new ArrayList<>(textureCount);
        List<short[]> indicesPerTexture = new ArrayList<>(textureCount);

        for (int i = 0; i < textureCount; i++) {
            Texture texture = textures.get(i);

            List<Float> vertices = new LinkedList<>();
            List<Short> indices = new LinkedList<>();

            for (int dx = 0; dx < ChunkSize.X; dx++) {
                for (int dy = 0; dy < ChunkSize.Y; dy++) {
                    for (int dz = 0; dz < ChunkSize.Z; dz++) {
                        generateMeshForBlockFromAtlas(vertices, indices, texture, chunkBlocks,
                                chunkX, chunkY, chunkZ,
                                dx, dy, dz);
                    }
                }
            }
            verticesPerTexture.add(convertToFloatArray(vertices));
            indicesPerTexture.add(convertToShortArray(indices));
        }

        return new ChunkMeshLists(8, verticesPerTexture, indicesPerTexture);
    }

    public List<MeshPart> generateMeshParts(ChunkMeshLists chunkMeshLists) {
        List<MeshPart> result = new LinkedList<>();
        int textureCount = chunkMeshLists.verticesPerTexture.size();
        for (int i = 0; i < textureCount; i++) {
            float[] vertices = chunkMeshLists.verticesPerTexture.get(i);
            short[] indices = chunkMeshLists.indicesPerTexture.get(i);

            if (indices.length > 0) {
                Mesh mesh = new Mesh(true, vertices.length / chunkMeshLists.floatsPerVertex, indices.length, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0));
                mesh.setVertices(vertices);
                mesh.setIndices(indices);

                MeshPart meshPart = new MeshPart("chunk", mesh, 0, indices.length, GL20.GL_TRIANGLES);

                result.add(meshPart);
            } else {
                result.add(null);
            }
        }
        return result;
    }

    public void generateMeshForBlockFromAtlas(List<Float> vertices, List<Short> indices, Texture texture, ChunkBlocks chunkBlocks,
                                              int chunkX, int chunkY, int chunkZ,
                                              int x, int y, int z) {
        String block = chunkBlocks.getCommonBlockAt(x, y, z);
        if (hasTextureAndShape(block)) {
            Map<String, String> availableTextures = getTextureParts(block);
            String shapeId = getShapeId(block);

            ShapeDef shape = shapeProvider.getShapeById(shapeId);
            for (ShapePartDef shapePart : shape.getShapeParts()) {
                String side = shapePart.getSide();

                BlockSide blockSide = BlockSide.valueOf(side);
                if (blockSide != null) {
                    // We need to check if block next to it is full (covers whole block side)
                    if (isNeighbourBlockCoveringSide(chunkBlocks, x, y, z, blockSide))
                        continue;
                }

                List<String> textureIds = shapePart.getTextures();

                String textureToUse = findFirstTexture(textureIds, availableTextures);
                TextureRegion textureRegion = textureAtlasProvider.getTexture(textureToUse);
                if (textureRegion.getTexture() == texture) {
                    int vertexCount = shapePart.getVertices().size();

                    // This array will store indexes of vertices in the resulting Mesh
                    short[] vertexMapping = new short[vertexCount];
                    for (int i = 0; i < vertexCount; i++) {
                        vertexMapping[i] = (short) (vertices.size() / 8);

                        Float[] vertexCoords = shapePart.getVertices().get(i);
                        Float[] normalValues = shapePart.getNormals().get(i);
                        Float[] textureCoords = shapePart.getUvs().get(i);

                        vertices.add(chunkX + x + vertexCoords[0]);
                        vertices.add(chunkY + y + vertexCoords[1]);
                        vertices.add(chunkZ + z + vertexCoords[2]);
                        vertices.add(normalValues[0]);
                        vertices.add(normalValues[1]);
                        vertices.add(normalValues[2]);
                        vertices.add(textureRegion.getU() + textureCoords[0] * (textureRegion.getU2() - textureRegion.getU()));
                        vertices.add(textureRegion.getV() + textureCoords[1] * (textureRegion.getV2() - textureRegion.getV()));
                    }
                    for (short index : shapePart.getIndices()) {
                        indices.add(vertexMapping[index]);
                    }
                }
            }
        }
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

    private boolean isNeighbourBlockCoveringSide(ChunkBlocks chunkBlocks, int x, int y, int z, BlockSide blockSide) {
        int resultX = x + blockSide.getNormalX();
        int resultY = y + blockSide.getNormalY();
        int resultZ = z + blockSide.getNormalZ();

        // If it's outside of chunk, we don't bother checking
        if (isOutsideOfChunk(resultX, resultY, resultZ)) {
            return false;
        }

        String neighbouringBlock = chunkBlocks.getCommonBlockAt(resultX, resultY, resultZ);
        if (hasTextureAndShape(neighbouringBlock)) {
            if (isTextureOpaque(neighbouringBlock)) {
                ShapeDef neighbourShapeDef = shapeProvider.getShapeById(getShapeId(neighbouringBlock));
                if (neighbourShapeDef.getFullParts().contains(blockSide.getOpposite().name())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOutsideOfChunk(int resultX, int resultY, int resultZ) {
        return resultX < 0 || resultX >= ChunkSize.X
                || resultY < 0 || resultY >= ChunkSize.Y
                || resultZ < 0 || resultZ >= ChunkSize.Z;
    }

    private String findFirstTexture(List<String> textureIds, Map<String, String> availableTextures) {
        for (String textureId : textureIds) {
            String texture = availableTextures.get(textureId);
            if (texture != null)
                return texture.substring(0, texture.lastIndexOf('.'));
        }

        return null;
    }

    private boolean isTextureOpaque(String commonBlockId) {
        return (Boolean) commonBlockManager.getCommonBlockById(commonBlockId).getComponents().
                get(shapeAndTextureComponentName).getFields().get("opaque");
    }

    private Map<String, String> getTextureParts(String commonBlockId) {
        return (Map<String, String>) commonBlockManager.getCommonBlockById(commonBlockId).getComponents().
                get(shapeAndTextureComponentName).getFields().get("parts");
    }

    private String getShapeId(String commonBlockId) {
        return (String) commonBlockManager.getCommonBlockById(commonBlockId).getComponents().
                get(shapeAndTextureComponentName).getFields().get("shapeId");
    }

    private boolean hasTextureAndShape(String commonBlockId) {
        return commonBlockManager.getCommonBlockById(commonBlockId).getComponents().
                get(shapeAndTextureComponentName) != null;
    }
}
