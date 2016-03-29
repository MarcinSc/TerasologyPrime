package com.gempukku.terasology.trees.leaves;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.trees.LSystemTreeBlockGeometryGenerator;
import com.gempukku.terasology.trees.component.LeavesDefinitionComponent;
import com.gempukku.terasology.trees.model.BranchDefinition;
import com.gempukku.terasology.trees.model.BranchSegmentDefinition;
import com.gempukku.terasology.world.chunk.geometry.BlockGeometryGenerator;
import org.terasology.math.geom.Matrix4f;
import org.terasology.math.geom.Vector3f;

import java.util.HashSet;
import java.util.Set;

@RegisterSystem(
        profiles = "generateChunkGeometry")
public class LastSegmentLeavesGenerator implements LeavesGenerator, LifeCycleSystem {
    @In
    private LeavesGeneratorRegistry leavesGeneratorRegistry;
    @In
    private PrefabManager prefabManager;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private ShapeProvider shapeProvider;

    @Override
    public void initialize() {
        leavesGeneratorRegistry.registerLeavesGenerator("lastSegment", this);

        Set<String> texturesToLoad = new HashSet<>();
        for (EntityData entityData : prefabManager.findPrefabsWithComponents(LeavesDefinitionComponent.class)) {
            ComponentData component = entityData.getComponent(LeavesDefinitionComponent.class);
            texturesToLoad.add((String) component.getFields().get("leavesTexture"));
        }
        textureAtlasRegistry.registerTextures(texturesToLoad);
    }

    @Override
    public LSystemTreeBlockGeometryGenerator.LSystemCallback createLeavesCallback(
            EntityRef entityRef, BlockGeometryGenerator.VertexOutput vertexOutput, Texture texture) {
        LeavesDefinitionComponent leavesDefinition = entityRef.getComponent(LeavesDefinitionComponent.class);
        TextureRegion leavesTexture = textureAtlasProvider.getTexture(leavesDefinition.getLeavesTexture());
        if (texture == leavesTexture.getTexture()) {
            return new LeavesDrawingCallback(vertexOutput, shapeProvider.getShapeById(leavesDefinition.getLeavesShape()), leavesTexture);
        } else {
            return null;
        }
    }

    private class LeavesDrawingCallback implements LSystemTreeBlockGeometryGenerator.LSystemCallback {
        private BlockGeometryGenerator.VertexOutput vertexOutput;

        private Vector3f tempVector = new Vector3f();
        private Vector3f origin = new Vector3f();
        private ShapeDef leavesShape;
        private TextureRegion texture;

        public LeavesDrawingCallback(BlockGeometryGenerator.VertexOutput vertexOutput, ShapeDef leavesShape, TextureRegion texture) {
            this.vertexOutput = vertexOutput;
            this.leavesShape = leavesShape;
            this.texture = texture;
        }

        @Override
        public void branchStart(boolean trunk, int segmentCount, BranchDefinition branch, Matrix4f movingMatrix) {

        }

        @Override
        public void segmentStart(boolean trunk, int segmentIndex, int segmentCount, BranchSegmentDefinition segment, Matrix4f movingMatrix) {

        }

        @Override
        public void segmentEnd(boolean trunk, int segmentIndex, int segmentCount, BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4f movingMatrix) {
            movingMatrix.transformPoint(origin.set(0, 0, 0));

            if (segmentIndex == segmentCount - 1) {
                float horizontalScale = trunk ? 0.7f : segmentCount * 0.7f;
                float verticalScale = horizontalScale * (trunk ? 1.2f : 0.6f);

                for (ShapePartDef shapePart : leavesShape.getShapeParts()) {
                    int vertexCount = shapePart.getVertices().size();

                    // This array will store indexes of vertices in the resulting Mesh
                    short[] vertexMapping = new short[vertexCount];

                    // Trunk
                    for (int vertex = 0; vertex < vertexCount; vertex++) {
                        Float[] vertexCoords = shapePart.getVertices().get(vertex);
                        Float[] normalValues = shapePart.getNormals().get(vertex);
                        Float[] textureCoords = shapePart.getUvs().get(vertex);

                        tempVector.set(vertexCoords[0] - 0.5f, vertexCoords[1] - 0.5f, vertexCoords[2] - 0.5f)
                                .mul(horizontalScale, verticalScale, horizontalScale).add(origin);

                        vertexOutput.setPosition(tempVector.x, tempVector.y, tempVector.z);
                        vertexOutput.setNormal(normalValues[0], normalValues[1], normalValues[2]);
                        vertexOutput.setTextureCoordinate(
                                texture.getU() + textureCoords[0] * (texture.getU2() - texture.getU()),
                                texture.getV() + textureCoords[1] * (texture.getV2() - texture.getV()));
                        vertexOutput.setFlag(BlockGeometryGenerator.MOVING_ON_WIND
                                + BlockGeometryGenerator.DOES_NOT_PRODUCE_GEOMETRY);

                        vertexMapping[vertex] = vertexOutput.finishVertex();
                    }
                    for (short index : shapePart.getIndices()) {
                        vertexOutput.addVertexIndex(vertexMapping[index]);
                    }
                }
            }
        }

        @Override
        public void branchEnd(boolean trunk, int segmentCount, BranchDefinition branch, Matrix4f movingMatrix) {

        }
    }
}
