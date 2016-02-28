package com.gempukku.terasology.graphics.environment.renderer;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.world.CommonBlockManager;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.component.ShapeAndTextureComponent;

import java.util.List;
import java.util.Map;

public class BlockMeshGenerator {
    private CommonBlockManager commonBlockManager;
    private WorldStorage worldStorage;
    private TextureAtlasProvider textureAtlasProvider;
    private ShapeProvider shapeProvider;

    private String shapeAndTextureComponentName;

    public BlockMeshGenerator(CommonBlockManager commonBlockManager, WorldStorage worldStorage, TextureAtlasProvider textureAtlasProvider,
                              TerasologyComponentManager terasologyComponentManager, ShapeProvider shapeProvider) {
        this.commonBlockManager = commonBlockManager;
        this.worldStorage = worldStorage;
        this.textureAtlasProvider = textureAtlasProvider;
        this.shapeProvider = shapeProvider;

        shapeAndTextureComponentName = terasologyComponentManager.getNameByComponent(ShapeAndTextureComponent.class);
    }

    public void generateCustomMeshForBlock(ModelBuilder modelBuilder, String worldId, int x, int y, int z) {
        WorldStorage.EntityRefOrCommonBlockId block = worldStorage.getBlockEntityOrBlockIdAt(worldId, x, y, z);

        // TODO Check if the entity for the block has a component that has special mesh generation
    }

    public void generateMeshForBlockFromAtlas(List<Float> vertices, List<Short> indices, Texture texture, String worldId, int x, int y, int z) {
        WorldStorage.EntityRefOrCommonBlockId block = worldStorage.getBlockEntityOrBlockIdAt(worldId, x, y, z);

        if (hasTextureAndShape(block)) {
            Map<String, String> availableTextures = getTextureParts(block);
            String shapeId = getShapeId(block);

            ShapeDef shape = shapeProvider.getShapeById(shapeId);
            for (ShapePartDef shapePart : shape.getShapeParts()) {
                String side = shapePart.getSide();

                BlockSide blockSide = BlockSide.valueOf(side);
                if (blockSide != null) {
                    // We need to check if block next to it is full (covers whole block side)
                    if (isNeighbourBlockCoveringSide(worldId, x, y, z, blockSide))
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

                        vertices.add(x + vertexCoords[0]);
                        vertices.add(y + vertexCoords[1]);
                        vertices.add(z + vertexCoords[2]);
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

    private boolean isNeighbourBlockCoveringSide(String worldId, int x, int y, int z, BlockSide blockSide) {
        WorldStorage.EntityRefOrCommonBlockId neighbouringBlock = worldStorage.getBlockEntityOrBlockIdAt(worldId,
                x + blockSide.getNormalX(),
                y + blockSide.getNormalY(),
                z + blockSide.getNormalZ());
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

    private String findFirstTexture(List<String> textureIds, Map<String, String> availableTextures) {
        for (String textureId : textureIds) {
            String texture = availableTextures.get(textureId);
            if (texture != null)
                return texture.substring(0, texture.lastIndexOf('.'));
        }

        return null;
    }

    private boolean isTextureOpaque(WorldStorage.EntityRefOrCommonBlockId block) {
        if (block.entityRef != null)
            return block.entityRef.getComponent(ShapeAndTextureComponent.class).isOpaque();
        else
            return (Boolean) commonBlockManager.getCommonBlockById(block.commonBlockId).getComponents().
                    get(shapeAndTextureComponentName).getFields().get("opaque");
    }

    private Map<String, String> getTextureParts(WorldStorage.EntityRefOrCommonBlockId block) {
        if (block.entityRef != null)
            return block.entityRef.getComponent(ShapeAndTextureComponent.class).getParts();
        else
            return (Map<String, String>) commonBlockManager.getCommonBlockById(block.commonBlockId).getComponents().
                    get(shapeAndTextureComponentName).getFields().get("parts");
    }

    private String getShapeId(WorldStorage.EntityRefOrCommonBlockId block) {
        if (block.entityRef != null)
            return block.entityRef.getComponent(ShapeAndTextureComponent.class).getShapeId();
        else
            return (String) commonBlockManager.getCommonBlockById(block.commonBlockId).getComponents().
                    get(shapeAndTextureComponentName).getFields().get("shapeId");
    }

    private boolean hasTextureAndShape(WorldStorage.EntityRefOrCommonBlockId block) {
        if (block.entityRef != null)
            return block.entityRef.hasComponent(ShapeAndTextureComponent.class)
                    && block.entityRef.hasComponent(ShapeAndTextureComponent.class);
        else
            return commonBlockManager.getCommonBlockById(block.commonBlockId).getComponents().
                    get(shapeAndTextureComponentName) != null;
    }
}
