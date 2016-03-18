package com.gempukku.terasology.trees;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.graphics.environment.BlockMeshGenerator;
import com.gempukku.terasology.graphics.environment.BlockMeshGeneratorRegistry;
import com.gempukku.terasology.graphics.environment.ChunkMeshGeneratorCallback;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.procedural.FastRandom;
import com.gempukku.terasology.procedural.PDist;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkSize;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Matrix4f;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;

import java.util.Arrays;
import java.util.Iterator;

@RegisterSystem(
        profiles = "generateChunkMeshes")
public class LSystemTreeBlockMeshGenerator implements BlockMeshGenerator, LifeCycleSystem {
    @In
    private BlockMeshGeneratorRegistry blockMeshGeneratorRegistry;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private WorldStorage worldStorage;
    @In
    private ShapeProvider shapeProvider;

    @Override
    public void initialize() {
        textureAtlasRegistry.registerTextures(
                Arrays.asList(
                        "blockTiles/trees/OakBark.png",
                        "blockTiles/plant/leaf/GreenLeaf.png"));
        blockMeshGeneratorRegistry.registerBlockMeshGenerator("trees:tree", this);
    }

    @Override
    public void generateMeshForBlockFromAtlas(ChunkMeshGeneratorCallback callback,
                                              VertexOutput vertexOutput, Texture texture,
                                              ChunkBlocks chunkBlocks, int xInChunk, int yInChunk, int zInChunk) {
        int treeX = chunkBlocks.x * ChunkSize.X + xInChunk;
        int treeY = chunkBlocks.y * ChunkSize.Y + yInChunk;
        int treeZ = chunkBlocks.z * ChunkSize.Z + zInChunk;

        WorldStorage.EntityRefAndCommonBlockId entityAndBlockId = worldStorage.getBlockEntityAndBlockIdAt(chunkBlocks.worldId,
                treeX,
                treeY,
                treeZ);

        TreeDefinition treeDefinition = createTreeDefinition(entityAndBlockId.entityRef, treeX, treeY, treeZ);

        TextureRegion barkTexture = textureAtlasProvider.getTexture(treeDefinition.barkTexture);
        if (texture == barkTexture.getTexture()) {
            Matrix4f movingMatrix = new Matrix4f(new Quat4f(), new Vector3f(
                    treeX + 0.5f,
                    treeY,
                    treeZ + 0.5f), 1);

            BranchDrawingCallback branchCallback = new BranchDrawingCallback(vertexOutput,
                    barkTexture);

            processBranchWithCallback(branchCallback, treeDefinition.trunkDefinition, movingMatrix);
        }
        TextureRegion leavesTexture = textureAtlasProvider.getTexture(treeDefinition.leavesTexture);
        if (texture == leavesTexture.getTexture()) {
            Matrix4f movingMatrix = new Matrix4f(new Quat4f(), new Vector3f(
                    treeX + 0.5f,
                    treeY,
                    treeZ + 0.5f), 1);

            LeavesDrawingCallback branchCallback = new LeavesDrawingCallback(vertexOutput, shapeProvider.getShapeById(treeDefinition.leavesShape),
                    leavesTexture);

            processBranchWithCallback(branchCallback, treeDefinition.trunkDefinition, movingMatrix);
        }
    }

    private TreeDefinition createTreeDefinition(EntityRef entity, int treeX, int treeY, int treeZ) {
        int seed = treeX + treeY * 173 + treeZ * 1543;
        FastRandom rnd = new FastRandom(seed);

        int generation = rnd.nextInt(20) + 1;

        PDist newTrunkSegmentLength = new PDist(0.8f, 0.2f, PDist.Type.normal);
        PDist newTrunkSegmentRadius = new PDist(0.02f, 0.005f, PDist.Type.normal);

        int maxBranchesPerSegment = 2;
        // 137.5 is a golden angle, which is incidentally the angle between two consecutive branches grown by many plants
        PDist branchInitialAngleAddY = new PDist(137.5f, 10f, PDist.Type.normal);
        PDist branchInitialAngleZ = new PDist(60, 20, PDist.Type.normal);
        PDist branchInitialLength = new PDist(0.3f, 0.075f, PDist.Type.normal);
        PDist branchInitialRadius = new PDist(0.01f, 0.003f, PDist.Type.normal);

        PDist segmentRotateX = new PDist(0, 15, PDist.Type.normal);
        PDist segmentRotateZ = new PDist(0, 15, PDist.Type.normal);

        PDist branchCurveAngleZ = new PDist(-8, 2, PDist.Type.normal);

        float segmentLengthIncreasePerGeneration = 0.2f;
        float segmentRadiusIncreasePerGeneration = 0.05f;

        float branchSegmentLengthIncreasePerGeneration = 0.1f;
        float branchSegmentRadiusIncreasePerGeneration = 0.05f;

        float trunkRotation = rnd.nextFloat();

        BranchDefinition tree = new BranchDefinition(0, trunkRotation);
        for (int i = 0; i < generation; i++) {
            float lastBranchAngle = 0;

            // Grow existing segments and their branches
            for (BranchSegmentDefinition segment : tree.segments) {
                segment.length += segmentLengthIncreasePerGeneration;
                segment.radius += segmentRadiusIncreasePerGeneration;
                for (BranchDefinition branch : segment.branches) {
                    lastBranchAngle += branch.rotationY;
                    for (BranchSegmentDefinition branchSegment : branch.segments) {
                        branchSegment.length += branchSegmentLengthIncreasePerGeneration;
                        branchSegment.radius += branchSegmentRadiusIncreasePerGeneration;
                    }

                    branch.segments.add(new BranchSegmentDefinition(
                            branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0,
                            branchCurveAngleZ.getValue(rnd)));
                }
            }

            int existingSegmentCount = tree.segments.size();

            if (existingSegmentCount > 2) {
                // Add branches to last existing segment
                int branchCount = rnd.nextInt(maxBranchesPerSegment) + 1;

                for (int branch = 0; branch < branchCount; branch++) {
                    lastBranchAngle = lastBranchAngle + branchInitialAngleAddY.getValue(rnd);
                    BranchDefinition branchDef = new BranchDefinition(
                            lastBranchAngle, branchInitialAngleZ.getValue(rnd));
                    BranchSegmentDefinition segment = new BranchSegmentDefinition(
                            branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0, 0);

                    branchDef.segments.add(segment);

                    tree.segments.get(existingSegmentCount - 1).branches.add(branchDef);
                }
            }

            // Add new segment
            BranchSegmentDefinition segment = new BranchSegmentDefinition(
                    newTrunkSegmentLength.getValue(rnd), newTrunkSegmentRadius.getValue(rnd),
                    segmentRotateX.getValue(rnd), segmentRotateZ.getValue(rnd));

            tree.segments.add(segment);
        }

        // Add leaves
        BranchSegmentDefinition lastTrunkSegment = tree.segments.get(tree.segments.size() - 1);
        lastTrunkSegment.horizontalLeavesScale = (float) Math.pow(tree.segments.size(), 1.3f) * 0.25f;
        lastTrunkSegment.verticalLeavesScale = lastTrunkSegment.horizontalLeavesScale * 0.75f;

        for (BranchSegmentDefinition segment : tree.segments) {
            for (BranchDefinition branch : segment.branches) {
                BranchSegmentDefinition lastBrunchSegment = branch.segments.get(branch.segments.size() - 1);

                lastBrunchSegment.horizontalLeavesScale = (float) Math.pow(tree.segments.size(), 1.3f) * 0.25f;
                lastBrunchSegment.verticalLeavesScale = lastBrunchSegment.horizontalLeavesScale * 0.75f;
            }
        }

        return new TreeDefinition("cube", "trees/blockTiles/trees/OakBark", "core/blockTiles/plant/leaf/GreenLeaf", tree);
    }

    private class LeavesDrawingCallback implements LSystemCallback {
        private VertexOutput vertexOutput;

        private Vector3f tempVector = new Vector3f();
        private Vector3f origin = new Vector3f();
        private ShapeDef leavesShape;
        private TextureRegion texture;

        public LeavesDrawingCallback(VertexOutput vertexOutput, ShapeDef leavesShape, TextureRegion texture) {
            this.vertexOutput = vertexOutput;
            this.leavesShape = leavesShape;
            this.texture = texture;
        }

        @Override
        public void branchStart(BranchDefinition branch, Matrix4f movingMatrix) {

        }

        @Override
        public void segmentStart(BranchSegmentDefinition segment, Matrix4f movingMatrix) {

        }

        @Override
        public void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4f movingMatrix) {
            movingMatrix.transformPoint(origin.set(0, 0, 0));

            if (segment.horizontalLeavesScale > 0.01f && segment.verticalLeavesScale > 0.01f) {
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
                                .mul(segment.horizontalLeavesScale, segment.verticalLeavesScale, segment.horizontalLeavesScale).add(origin);

                        vertexOutput.setPosition(tempVector.x, tempVector.y, tempVector.z);
                        vertexOutput.setNormal(normalValues[0], normalValues[1], normalValues[2]);
                        vertexOutput.setTextureCoordinate(
                                texture.getU() + textureCoords[0] * (texture.getU2() - texture.getU()),
                                texture.getV() + textureCoords[1] * (texture.getV2() - texture.getV()));
                        vertexOutput.setFlag(INFLUENCED_BY_WIND);

                        vertexMapping[vertex] = vertexOutput.finishVertex();
                    }
                    for (short index : shapePart.getIndices()) {
                        vertexOutput.addVertexIndex(vertexMapping[index]);
                    }
                }
            }
        }

        @Override
        public void branchEnd(BranchDefinition branch, Matrix4f movingMatrix) {

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

        private VertexOutput vertexOutput;
        private TextureRegion texture;

        public BranchDrawingCallback(VertexOutput vertexOutput, TextureRegion texture) {
            this.vertexOutput = vertexOutput;
            this.texture = texture;
        }

        @Override
        public void branchStart(BranchDefinition branch, Matrix4f movingMatrix) {

        }

        @Override
        public void segmentStart(BranchSegmentDefinition segment, Matrix4f movingMatrix) {
            movingMatrix.transformPoint(first.set(1, 0, 1).mul(segment.radius));
            movingMatrix.transformPoint(second.set(-1, 0, 1).mul(segment.radius));
            movingMatrix.transformPoint(third.set(-1, 0, -1).mul(segment.radius));
            movingMatrix.transformPoint(fourth.set(1, 0, -1).mul(segment.radius));
        }

        @Override
        public void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4f movingMatrix) {
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
        public void branchEnd(BranchDefinition branch, Matrix4f movingMatrix) {

        }
    }

    private void processBranchWithCallback(LSystemCallback callback, BranchDefinition branchDefinition, Matrix4f movingMatrix) {
        movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(0, 1, 0), TeraMath.DEG_TO_RAD * branchDefinition.rotationY), new Vector3f(), 1));
        movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(0, 0, 1), TeraMath.DEG_TO_RAD * branchDefinition.rotationZ), new Vector3f(), 1));

        callback.branchStart(branchDefinition, movingMatrix);

        Iterator<BranchSegmentDefinition> segmentIterator = branchDefinition.segments.iterator();
        BranchSegmentDefinition currentSegment = segmentIterator.next();
        do {
            BranchSegmentDefinition nextSegment = segmentIterator.hasNext() ? segmentIterator.next() : null;

            callback.segmentStart(currentSegment, movingMatrix);
            movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(0, 0, 1), TeraMath.DEG_TO_RAD * currentSegment.rotateZ), new Vector3f(), 1));
            movingMatrix.mul(new Matrix4f(new Quat4f(new Vector3f(1, 0, 0), TeraMath.DEG_TO_RAD * currentSegment.rotateX), new Vector3f(), 1));
            movingMatrix.mul(new Matrix4f(new Quat4f(), new Vector3f(0, currentSegment.length, 0), 1));
            callback.segmentEnd(currentSegment, nextSegment, movingMatrix);

            // Get back half of the segment to create branches
            movingMatrix.mul(new Matrix4f(new Quat4f(), new Vector3f(0, -currentSegment.length / 2, 0), 1));

            for (BranchDefinition branch : currentSegment.branches) {
                Matrix4f branchMatrix = new Matrix4f(movingMatrix);
                processBranchWithCallback(callback, branch, branchMatrix);
            }
            movingMatrix.mul(new Matrix4f(new Quat4f(), new Vector3f(0, currentSegment.length / 2, 0), 1));

            currentSegment = nextSegment;
        } while (currentSegment != null);

        callback.branchEnd(branchDefinition, movingMatrix);
    }

    private void addQuad(VertexOutput vertexOutput,
                         Vector3f normal,
                         Vector3f first, Vector3f second, Vector3f third, Vector3f fourth, TextureRegion texture) {

        vertexOutput.setPosition(first.x, first.y, first.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 1 * (texture.getU2() - texture.getU()),
                texture.getV() + 0 * (texture.getV2() - texture.getV()));
        short firstIndex = vertexOutput.finishVertex();

        vertexOutput.setPosition(second.x, second.y, second.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 1 * (texture.getU2() - texture.getU()),
                texture.getV() + 1 * (texture.getV2() - texture.getV()));
        short secondIndex = vertexOutput.finishVertex();

        vertexOutput.setPosition(third.x, third.y, third.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 0 * (texture.getU2() - texture.getU()),
                texture.getV() + 1 * (texture.getV2() - texture.getV()));
        short thirdIndex = vertexOutput.finishVertex();

        vertexOutput.setPosition(fourth.x, fourth.y, fourth.z);
        vertexOutput.setNormal(normal.x, normal.y, normal.z);
        vertexOutput.setTextureCoordinate(
                texture.getU() + 0 * (texture.getU2() - texture.getU()),
                texture.getV() + 0 * (texture.getV2() - texture.getV()));
        short fourthIndex = vertexOutput.finishVertex();

        vertexOutput.addVertexIndex(firstIndex);
        vertexOutput.addVertexIndex(secondIndex);
        vertexOutput.addVertexIndex(thirdIndex);
        vertexOutput.addVertexIndex(thirdIndex);
        vertexOutput.addVertexIndex(fourthIndex);
        vertexOutput.addVertexIndex(firstIndex);
    }

    private interface LSystemCallback {
        void branchStart(BranchDefinition branch, Matrix4f movingMatrix);

        void segmentStart(BranchSegmentDefinition segment, Matrix4f movingMatrix);

        void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4f movingMatrix);

        void branchEnd(BranchDefinition branch, Matrix4f movingMatrix);
    }
}
