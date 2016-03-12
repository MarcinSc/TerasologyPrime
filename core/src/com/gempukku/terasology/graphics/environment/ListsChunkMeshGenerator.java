package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.prefab.PrefabComponentData;
import com.gempukku.terasology.prefab.PrefabData;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.component.ShapeAndTextureComponent;

import java.util.List;
import java.util.Map;

@RegisterSystem(
        profiles = "generateChunkMeshes", shared = ChunkMeshGenerator.class)
public class ListsChunkMeshGenerator implements ChunkMeshGenerator<ChunkMeshLists>, LifeCycleSystem {
    @In
    private ChunkBlocksProvider chunkBlocksProvider;
    @In
    private CommonBlockManager commonBlockManager;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private ShapeProvider shapeProvider;
    @In
    private TerasologyComponentManager terasologyComponentManager;

    private ShortArrayThreadLocal shorts = new ShortArrayThreadLocal();
    private FloatArrayThreadLocal floats = new FloatArrayThreadLocal();

    private ShapeDef[] shapesByBlockId;
    private Map<String, String>[] texturesByBlockId;
    private boolean[] opaqueByBlockId;

    private void init() {
        if (shapesByBlockId == null) {
            String shapeAndTextureComponentName = terasologyComponentManager.getNameByComponent(ShapeAndTextureComponent.class);

            int commonBlockCount = commonBlockManager.getCommonBlockCount();
            shapesByBlockId = new ShapeDef[commonBlockCount];
            texturesByBlockId = new Map[commonBlockCount];
            opaqueByBlockId = new boolean[commonBlockCount];
            for (short i = 0; i < commonBlockCount; i++) {
                PrefabData commonBlockData = commonBlockManager.getCommonBlockById(i);
                PrefabComponentData shapeAndTextureComponent = commonBlockData.getComponents().
                        get(shapeAndTextureComponentName);
                if (shapeAndTextureComponent != null) {
                    shapesByBlockId[i] = shapeProvider.getShapeById((String) shapeAndTextureComponent.getFields().get("shapeId"));
                    texturesByBlockId[i] = (Map<String, String>) shapeAndTextureComponent.getFields().get("parts");
                    opaqueByBlockId[i] = (Boolean) shapeAndTextureComponent.getFields().get("opaque");
                }
            }
        }
    }

    @Override
    public ChunkMeshLists prepareChunkDataOffThread(List<Texture> textures, String worldId, int x, int y, int z) {
        init();
        
        ChunkBlocks chunkBlocks = chunkBlocksProvider.getChunkBlocks(worldId, x, y, z);
        int chunkX = chunkBlocks.x * ChunkSize.X;
        int chunkY = chunkBlocks.y * ChunkSize.Y;
        int chunkZ = chunkBlocks.z * ChunkSize.Z;

        int textureCount = textures.size();

        float[][] verticesPerTexture = new float[textureCount][];
        short[][] indicesPerTexture = new short[textureCount][];

        for (int i = 0; i < textureCount; i++) {
            Texture texture = textures.get(i);

            FloatArray vertices = floats.get();
            vertices.clear();

            ShortArray indices = shorts.get();
            indices.clear();

            for (int dx = 0; dx < ChunkSize.X; dx++) {
                for (int dy = 0; dy < ChunkSize.Y; dy++) {
                    for (int dz = 0; dz < ChunkSize.Z; dz++) {
                        generateMeshForBlockFromAtlas(vertices, indices, texture, chunkBlocks,
                                chunkX, chunkY, chunkZ,
                                dx, dy, dz);
                    }
                }
            }
            verticesPerTexture[i] = vertices.toArray();
            indicesPerTexture[i] = indices.toArray();
        }

        return new ChunkMeshLists(8, verticesPerTexture, indicesPerTexture);
    }

    @Override
    public Array<MeshPart> generateMeshParts(ChunkMeshLists chunkMeshLists) {
        Array<MeshPart> result = new Array<>();
        int textureCount = chunkMeshLists.verticesPerTexture.length;
        for (int i = 0; i < textureCount; i++) {
            float[] vertices = chunkMeshLists.verticesPerTexture[i];
            short[] indices = chunkMeshLists.indicesPerTexture[i];

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

    private void generateMeshForBlockFromAtlas(FloatArray vertices, ShortArray indices, Texture texture, ChunkBlocks chunkBlocks,
                                               int chunkX, int chunkY, int chunkZ,
                                               int x, int y, int z) {
        short block = chunkBlocks.getCommonBlockAt(x, y, z);

        if (shapesByBlockId[block] != null && texturesByBlockId[block] != null) {
            Map<String, String> availableTextures = texturesByBlockId[block];

            ShapeDef shape = shapesByBlockId[block];
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
                        vertexMapping[i] = (short) (vertices.size / 8);

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

    private boolean isNeighbourBlockCoveringSide(ChunkBlocks chunkBlocks, int x, int y, int z, BlockSide blockSide) {
        int resultX = x + blockSide.getNormalX();
        int resultY = y + blockSide.getNormalY();
        int resultZ = z + blockSide.getNormalZ();

        // If it's outside of chunk, we don't bother checking
        if (isOutsideOfChunk(resultX, resultY, resultZ)) {
            return false;
        }

        short neighbouringBlock = chunkBlocks.getCommonBlockAt(resultX, resultY, resultZ);
        if (shapesByBlockId[neighbouringBlock] != null) {
            if (opaqueByBlockId[neighbouringBlock]) {
                ShapeDef neighbourShapeDef = shapesByBlockId[neighbouringBlock];
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

    private static class ShortArrayThreadLocal extends ThreadLocal<ShortArray> {
        @Override
        protected ShortArray initialValue() {
            return new ShortArray(1024);
        }
    }

    private static class FloatArrayThreadLocal extends ThreadLocal<FloatArray> {
        @Override
        protected FloatArray initialValue() {
            return new FloatArray(1024);
        }
    }
}
