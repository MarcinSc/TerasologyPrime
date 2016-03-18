package com.gempukku.terasology.graphics.environment;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.component.GeneratedBlockMeshComponent;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkBlocksProvider;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.component.ShapeAndTextureComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterSystem(
        profiles = "generateChunkMeshes", shared = {ChunkMeshGenerator.class, BlockMeshGeneratorRegistry.class})
public class ListsChunkMeshGenerator implements ChunkMeshGenerator<ChunkMeshLists>, ChunkMeshGeneratorCallback,
        BlockMeshGeneratorRegistry, LifeCycleSystem {
    @In
    private ChunkBlocksProvider chunkBlocksProvider;
    @In
    private CommonBlockManager commonBlockManager;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private ShapeProvider shapeProvider;

    private ShortArrayThreadLocal shorts = new ShortArrayThreadLocal();
    private FloatArrayThreadLocal floats = new FloatArrayThreadLocal();

    private ShapeDef[] shapesByBlockId;
    private Map<String, String>[] texturesByBlockId;
    private boolean[] opaqueByBlockId;
    private String[] blockMeshGenerators;

    private Map<String, BlockMeshGenerator> registeredBlockMeshGenerators = new HashMap<>();

    private final int[][] blockSector = new int[][]
            {
                    {-1, -1, -1}, {-1, -1, 0}, {-1, -1, 1},
                    {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1},
                    {-1, 1, -1}, {-1, 1, 0}, {-1, 1, 1},
                    {0, -1, -1}, {0, -1, 0}, {0, -1, 1},
                    {0, 0, -1}, {0, 0, 0}, {0, 0, 1},
                    {0, 1, -1}, {0, 1, 0}, {0, 1, 1},
                    {1, -1, -1}, {1, -1, 0}, {1, -1, 1},
                    {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
                    {1, 1, -1}, {1, 1, 0}, {1, 1, 1}
            };

    @Override
    public void registerBlockMeshGenerator(String generatorType, BlockMeshGenerator generator) {
        registeredBlockMeshGenerators.put(generatorType, generator);
    }

    private void init() {
        if (shapesByBlockId == null) {
            int commonBlockCount = commonBlockManager.getCommonBlockCount();
            shapesByBlockId = new ShapeDef[commonBlockCount];
            texturesByBlockId = new Map[commonBlockCount];
            opaqueByBlockId = new boolean[commonBlockCount];
            blockMeshGenerators = new String[commonBlockCount];
            for (short i = 0; i < commonBlockCount; i++) {
                EntityData commonBlockData = commonBlockManager.getCommonBlockById(i);
                ComponentData shapeAndTextureComponent = commonBlockData.getComponent(ShapeAndTextureComponent.class);
                if (shapeAndTextureComponent != null) {
                    shapesByBlockId[i] = shapeProvider.getShapeById((String) shapeAndTextureComponent.getFields().get("shapeId"));
                    texturesByBlockId[i] = (Map<String, String>) shapeAndTextureComponent.getFields().get("parts");
                    opaqueByBlockId[i] = (Boolean) shapeAndTextureComponent.getFields().get("opaque");
                }
                ComponentData generatedBlockMeshComponent = commonBlockData.getComponent(GeneratedBlockMeshComponent.class);
                if (generatedBlockMeshComponent != null) {
                    blockMeshGenerators[i] = (String) generatedBlockMeshComponent.getFields().get("generatorType");
                }
            }
        }
    }

    @Override
    public boolean canPrepareChunkData(String worldId, int x, int y, int z) {
        for (int[] surroundingChunk : blockSector) {
            if (chunkBlocksProvider.getChunkBlocks(worldId, x + surroundingChunk[0], y + surroundingChunk[1], z + surroundingChunk[2]) == null)
                return false;
        }
        return true;
    }

    @Override
    public ChunkMeshLists prepareChunkDataOffThread(List<Texture> textures, String worldId, int x, int y, int z) {
        init();

        ChunkBlocks[] chunkSector = new ChunkBlocks[blockSector.length];

        for (int i = 0; i < blockSector.length; i++) {
            chunkSector[i] = chunkBlocksProvider.getChunkBlocks(worldId,
                    x + blockSector[i][0], y + blockSector[i][1], z + blockSector[i][2]);
        }

        int chunkX = x * ChunkSize.X;
        int chunkY = y * ChunkSize.Y;
        int chunkZ = z * ChunkSize.Z;

        int textureCount = textures.size();

        float[][] verticesPerTexture = new float[textureCount][];
        short[][] indicesPerTexture = new short[textureCount][];

        for (int i = 0; i < textureCount; i++) {
            Texture texture = textures.get(i);

            FloatArray vertices = floats.get();
            vertices.clear();

            ShortArray indices = shorts.get();
            indices.clear();

            BlockMeshGenerator.VertexOutput vertexOutput = new ArrayVertexOutput(vertices, indices);

            for (int dx = 0; dx < ChunkSize.X; dx++) {
                for (int dy = 0; dy < ChunkSize.Y; dy++) {
                    for (int dz = 0; dz < ChunkSize.Z; dz++) {
                        generateMeshForBlockFromAtlas(vertexOutput, texture, chunkSector,
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
                VertexAttribute customVertexInformation = new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_flag");

                Mesh mesh = new Mesh(true, vertices.length / chunkMeshLists.floatsPerVertex, indices.length,
                        VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0),
                        customVertexInformation);
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

    private void generateMeshForBlockFromAtlas(BlockMeshGenerator.VertexOutput vertexOutput, Texture texture, ChunkBlocks[] chunkSector,
                                               int chunkX, int chunkY, int chunkZ,
                                               int x, int y, int z) {
        short block = chunkSector[13].getCommonBlockAt(x, y, z);

        if (shapesByBlockId[block] != null && texturesByBlockId[block] != null) {
            Map<String, String> availableTextures = texturesByBlockId[block];

            ShapeDef shape = shapesByBlockId[block];
            for (ShapePartDef shapePart : shape.getShapeParts()) {
                BlockSide blockSide = shapePart.getSide();

                if (blockSide != null) {
                    // We need to check if block next to it is full (covers whole block side)
                    if (isNeighbourBlockCoveringSide(chunkSector, x, y, z, blockSide))
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
                        Float[] vertexCoords = shapePart.getVertices().get(i);
                        Float[] normalValues = shapePart.getNormals().get(i);
                        Float[] textureCoords = shapePart.getUvs().get(i);

                        vertexOutput.setPosition(
                                chunkX + x + vertexCoords[0],
                                chunkY + y + vertexCoords[1],
                                chunkZ + z + vertexCoords[2]);
                        vertexOutput.setNormal(
                                normalValues[0],
                                normalValues[1],
                                normalValues[2]);
                        vertexOutput.setTextureCoordinate(
                                textureRegion.getU() + textureCoords[0] * (textureRegion.getU2() - textureRegion.getU()),
                                textureRegion.getV() + textureCoords[1] * (textureRegion.getV2() - textureRegion.getV()));

                        vertexMapping[i] = vertexOutput.finishVertex();

                    }
                    for (short index : shapePart.getIndices()) {
                        vertexOutput.addVertexIndex(vertexMapping[index]);
                    }
                }
            }
        } else if (blockMeshGenerators[block] != null) {
            BlockMeshGenerator blockMeshGenerator = registeredBlockMeshGenerators.get(blockMeshGenerators[block]);
            blockMeshGenerator.generateMeshForBlockFromAtlas(this, vertexOutput, texture, chunkSector[13],
                    x, y, z);
        }
    }

    @Override
    public boolean isNeighbourBlockCoveringSide(ChunkBlocks[] chunkSector, int x, int y, int z, BlockSide blockSide) {
        int resultX = x + blockSide.getNormalX();
        int resultY = y + blockSide.getNormalY();
        int resultZ = z + blockSide.getNormalZ();

        int chunkPositionInSector = 13;

        if (resultX < 0) {
            chunkPositionInSector -= 9;
            resultX += ChunkSize.X;
        } else if (resultX >= ChunkSize.X) {
            chunkPositionInSector += 9;
            resultX -= ChunkSize.X;
        }
        if (resultY < 0) {
            chunkPositionInSector -= 3;
            resultY += ChunkSize.Y;
        } else if (resultY >= ChunkSize.Y) {
            chunkPositionInSector += 3;
            resultY -= ChunkSize.Y;
        }
        if (resultZ < 0) {
            chunkPositionInSector -= 1;
            resultZ += ChunkSize.Z;
        } else if (resultZ >= ChunkSize.Z) {
            chunkPositionInSector += 1;
            resultZ -= ChunkSize.Z;
        }

        short neighbouringBlock = chunkSector[chunkPositionInSector].getCommonBlockAt(resultX, resultY, resultZ);
        if (shapesByBlockId[neighbouringBlock] != null) {
            if (opaqueByBlockId[neighbouringBlock]) {
                ShapeDef neighbourShapeDef = shapesByBlockId[neighbouringBlock];
                if (neighbourShapeDef.getFullParts().contains(blockSide.getOpposite())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String findFirstTexture(List<String> textureIds, Map<String, String> availableTextures) {
        for (String textureId : textureIds) {
            String texture = availableTextures.get(textureId);
            if (texture != null)
                return texture;
        }

        return null;
    }

    private static class ArrayVertexOutput implements BlockMeshGenerator.VertexOutput {
        private FloatArray vertices;
        private ShortArray indices;

        private short vertexIndex;
        private float x;
        private float y;
        private float z;
        private float normalX;
        private float normalY;
        private float normalZ;
        private float textureCoordX;
        private float textureCoordY;
        private int flag;

        public ArrayVertexOutput(FloatArray vertices, ShortArray indices) {
            this.vertices = vertices;
            this.indices = indices;
        }

        @Override
        public short finishVertex() {
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);
            vertices.add(normalX);
            vertices.add(normalY);
            vertices.add(normalZ);
            vertices.add(textureCoordX);
            vertices.add(textureCoordY);
            vertices.add(flag);//Float.intBitsToFloat(flag));

            x = y = z = normalX = normalY = normalZ = textureCoordX = textureCoordY = flag = 0;

            return vertexIndex++;
        }

        @Override
        public void setNormal(float x, float y, float z) {
            normalX = x;
            normalY = y;
            normalZ = z;
        }

        @Override
        public void setPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void setTextureCoordinate(float x, float y) {
            textureCoordX = x;
            textureCoordY = y;
        }

        @Override
        public void setFlag(int flag) {
            this.flag = flag;
        }

        @Override
        public void addVertexIndex(short vertexIndex) {
            indices.add(vertexIndex);
        }
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
