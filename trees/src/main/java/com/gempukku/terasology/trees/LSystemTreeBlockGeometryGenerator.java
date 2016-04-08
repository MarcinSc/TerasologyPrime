package com.gempukku.terasology.trees;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.graphics.environment.mesh.ChunkMeshGeneratorCallback;
import com.gempukku.terasology.trees.component.TreeGenerationComponent;
import com.gempukku.terasology.trees.leaves.LeavesGenerator;
import com.gempukku.terasology.trees.leaves.LeavesGeneratorRegistry;
import com.gempukku.terasology.trees.model.BranchDefinition;
import com.gempukku.terasology.trees.model.BranchSegmentDefinition;
import com.gempukku.terasology.trees.model.TreeDefinition;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkSize;
import com.gempukku.terasology.world.chunk.geometry.BlockGeometryGenerator;
import com.gempukku.terasology.world.chunk.geometry.BlockGeometryGeneratorRegistry;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Matrix4f;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RegisterSystem(
        profiles = "generateChunkGeometry", shared = {TreeGeneratorRegistry.class, LeavesGeneratorRegistry.class})
public class LSystemTreeBlockGeometryGenerator implements BlockGeometryGenerator, LifeCycleSystem,
        TreeGeneratorRegistry, LeavesGeneratorRegistry {
    @In
    private BlockGeometryGeneratorRegistry blockGeometryGeneratorRegistry;
    @In
    private WorldStorage worldStorage;

    private Map<String, TreeGenerator> treeGenerators = new HashMap<>();
    private Map<String, LeavesGenerator> leavesGenerators = new HashMap<>();

    @Override
    public void initialize() {
        blockGeometryGeneratorRegistry.registerBlockMeshGenerator("trees:tree", this);
    }

    @Override
    public void registerTreeGenerator(String generatorName, TreeGenerator treeGenerator) {
        treeGenerators.put(generatorName, treeGenerator);
    }

    @Override
    public void registerLeavesGenerator(String generatorName, LeavesGenerator leavesGenerator) {
        leavesGenerators.put(generatorName, leavesGenerator);
    }

    @Override
    public void generateGeometryForBlockFromAtlas(ChunkMeshGeneratorCallback callback,
                                                  BlockVertexOutput vertexOutput, Texture texture,
                                                  ChunkBlocks chunkBlocks, int xInChunk, int yInChunk, int zInChunk) {
        int treeX = chunkBlocks.x * ChunkSize.X + xInChunk;
        int treeY = chunkBlocks.y * ChunkSize.Y + yInChunk;
        int treeZ = chunkBlocks.z * ChunkSize.Z + zInChunk;

        vertexOutput.setBlock(treeX, treeY, treeZ);

        WorldStorage.EntityRefAndCommonBlockId entityAndBlockId = worldStorage.getBlockEntityAndBlockIdAt(chunkBlocks.worldId,
                treeX,
                treeY,
                treeZ);

        // It's possible that the chunk has been unloaded in the meantime
        if (entityAndBlockId != null & entityAndBlockId.entityRef != null) {
            TreeGenerationComponent treeGeneration = entityAndBlockId.entityRef.getComponent(TreeGenerationComponent.class);
            TreeDefinition treeDefinition = treeGenerators.get(treeGeneration.getGenerationType()).generateTreeDefinition(entityAndBlockId.entityRef);

            if (texture == treeDefinition.barkTexture.getTexture()) {
                Matrix4f movingMatrix = new Matrix4f(new Quat4f(), new Vector3f(
                        treeX + 0.5f,
                        treeY,
                        treeZ + 0.5f), 1);

                BranchDrawingCallback branchCallback = new BranchDrawingCallback(vertexOutput,
                        treeDefinition.barkTexture);

                processBranchWithCallback(branchCallback, true, treeDefinition.trunkDefinition, movingMatrix);
            }

            LeavesGenerator leavesGenerator = leavesGenerators.get(treeDefinition.leavesGenerator);
            LSystemCallback leavesCallback = leavesGenerator.createLeavesCallback(entityAndBlockId.entityRef, vertexOutput, texture);
            if (leavesCallback != null) {
                Matrix4f movingMatrix = new Matrix4f(new Quat4f(), new Vector3f(
                        treeX + 0.5f,
                        treeY,
                        treeZ + 0.5f), 1);

                processBranchWithCallback(leavesCallback, true, treeDefinition.trunkDefinition, movingMatrix);
            }
        }
    }

    private class BranchDrawingCallback implements LSystemCallback {
        private Vector3f normal = new Vector3f();

        private Vector3f first = new Vector3f();
        private Vector3f second = new Vector3f();
        private Vector3f third = new Vector3f();
        private Vector3f fourth = new Vector3f();

        private Vector3f firstTop = new Vector3f();
        private Vector3f secondTop = new Vector3f();
        private Vector3f thirdTop = new Vector3f();
        private Vector3f fourthTop = new Vector3f();

        private BlockVertexOutput vertexOutput;
        private TextureRegion texture;

        public BranchDrawingCallback(BlockVertexOutput vertexOutput, TextureRegion texture) {
            this.vertexOutput = vertexOutput;
            this.texture = texture;
        }

        @Override
        public void branchStart(boolean trunk, int segmentCount, BranchDefinition branch, Matrix4f movingMatrix) {

        }

        @Override
        public void segmentStart(boolean trunk, int segmentIndex, int segmentCount, BranchSegmentDefinition segment, Matrix4f movingMatrix) {
            movingMatrix.transformPoint(first.set(1, 0, 1).mul(segment.radius));
            movingMatrix.transformPoint(second.set(-1, 0, 1).mul(segment.radius));
            movingMatrix.transformPoint(third.set(-1, 0, -1).mul(segment.radius));
            movingMatrix.transformPoint(fourth.set(1, 0, -1).mul(segment.radius));
        }

        @Override
        public void segmentEnd(boolean trunk, int segmentIndex, int segmentCount, BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4f movingMatrix) {
            float radius;
            if (nextSegment != null)
                radius = nextSegment.radius;
            else
                radius = 0;

            movingMatrix.transformPoint(firstTop.set(1, 0, 1).mul(radius));
            movingMatrix.transformPoint(secondTop.set(-1, 0, 1).mul(radius));
            movingMatrix.transformPoint(thirdTop.set(-1, 0, -1).mul(radius));
            movingMatrix.transformPoint(fourthTop.set(1, 0, -1).mul(radius));

            movingMatrix.transformVector(normal.set(0, 0, 1));
            addQuad(vertexOutput, normal,
                    first, firstTop, secondTop, second, texture);

            movingMatrix.transformVector(normal.set(-1, 0, 0));
            addQuad(vertexOutput, normal,
                    second, secondTop, thirdTop, third, texture);

            movingMatrix.transformVector(normal.set(0, 0, -1));
            addQuad(vertexOutput, normal,
                    third, thirdTop, fourthTop, fourth, texture);

            movingMatrix.transformVector(normal.set(1, 0, 0));
            addQuad(vertexOutput, normal,
                    fourth, fourthTop, firstTop, first, texture);
        }

        @Override
        public void branchEnd(boolean trunk, int segmentCount, BranchDefinition branch, Matrix4f movingMatrix) {

        }
    }

    private void processBranchWithCallback(LSystemCallback callback, boolean trunk, BranchDefinition branchDefinition, Matrix4f movingMatrix) {
        movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(0, 1, 0), TeraMath.DEG_TO_RAD * branchDefinition.rotationY), new Vector3f(), 1));
        movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(0, 0, 1), TeraMath.DEG_TO_RAD * branchDefinition.rotationZ), new Vector3f(), 1));

        int segmentCount = branchDefinition.segments.size();
        callback.branchStart(trunk, segmentCount, branchDefinition, movingMatrix);

        Iterator<BranchSegmentDefinition> segmentIterator = branchDefinition.segments.iterator();
        BranchSegmentDefinition currentSegment = segmentIterator.next();
        int segmentIndex = 0;
        do {
            BranchSegmentDefinition nextSegment = segmentIterator.hasNext() ? segmentIterator.next() : null;

            callback.segmentStart(trunk, segmentIndex, segmentCount, currentSegment, movingMatrix);
            movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(0, 0, 1), TeraMath.DEG_TO_RAD * currentSegment.rotateZ), new Vector3f(), 1));
            movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(1, 0, 0), TeraMath.DEG_TO_RAD * currentSegment.rotateX), new Vector3f(), 1));
            movingMatrix.mul(new Matrix4f(new Quat4f(), new Vector3f(0, currentSegment.length, 0), 1));
            callback.segmentEnd(trunk, segmentIndex, segmentCount, currentSegment, nextSegment, movingMatrix);

            // Get back half of the segment to create branches
            movingMatrix.mul(new Matrix4f(new Quat4f(), new Vector3f(0, -currentSegment.length / 2, 0), 1));

            for (BranchDefinition branch : currentSegment.branches) {
                Matrix4f branchMatrix = new Matrix4f(movingMatrix);
                processBranchWithCallback(callback, false, branch, branchMatrix);
            }
            movingMatrix.mul(new Matrix4f(new Quat4f(), new Vector3f(0, currentSegment.length / 2, 0), 1));

            currentSegment = nextSegment;

            segmentIndex++;
        } while (currentSegment != null);

        callback.branchEnd(trunk, segmentCount, branchDefinition, movingMatrix);
    }

    private void addQuad(BlockVertexOutput vertexOutput,
                         Vector3f normal,
                         Vector3f first, Vector3f second, Vector3f third, Vector3f fourth, TextureRegion texture) {

        vertexOutput.setPosition(first.x, first.y, first.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 1 * (texture.getU2() - texture.getU()),
                texture.getV() + 0 * (texture.getV2() - texture.getV()));
        vertexOutput.setFlag(BlockGeometryGenerator.DOES_NOT_PRODUCE_GEOMETRY);
        short firstIndex = vertexOutput.finishVertex();

        vertexOutput.setPosition(second.x, second.y, second.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 1 * (texture.getU2() - texture.getU()),
                texture.getV() + 1 * (texture.getV2() - texture.getV()));
        vertexOutput.setFlag(BlockGeometryGenerator.DOES_NOT_PRODUCE_GEOMETRY);
        short secondIndex = vertexOutput.finishVertex();

        vertexOutput.setPosition(third.x, third.y, third.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 0 * (texture.getU2() - texture.getU()),
                texture.getV() + 1 * (texture.getV2() - texture.getV()));
        vertexOutput.setFlag(BlockGeometryGenerator.DOES_NOT_PRODUCE_GEOMETRY);
        short thirdIndex = vertexOutput.finishVertex();

        vertexOutput.setPosition(fourth.x, fourth.y, fourth.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 0 * (texture.getU2() - texture.getU()),
                texture.getV() + 0 * (texture.getV2() - texture.getV()));
        vertexOutput.setFlag(BlockGeometryGenerator.DOES_NOT_PRODUCE_GEOMETRY);
        short fourthIndex = vertexOutput.finishVertex();

        vertexOutput.addVertexIndex(firstIndex);
        vertexOutput.addVertexIndex(secondIndex);
        vertexOutput.addVertexIndex(thirdIndex);
        vertexOutput.addVertexIndex(thirdIndex);
        vertexOutput.addVertexIndex(fourthIndex);
        vertexOutput.addVertexIndex(firstIndex);
    }

    public interface LSystemCallback {
        void branchStart(boolean trunk, int segmentCount, BranchDefinition branch, Matrix4f movingMatrix);

        void segmentStart(boolean trunk, int segmentIndex, int segmentCount, BranchSegmentDefinition segment, Matrix4f movingMatrix);

        void segmentEnd(boolean trunk, int segmentIndex, int segmentCount, BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4f movingMatrix);

        void branchEnd(boolean trunk, int segmentCount, BranchDefinition branch, Matrix4f movingMatrix);
    }
}
