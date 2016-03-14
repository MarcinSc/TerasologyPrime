package com.gempukku.terasology.trees;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
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
import com.gempukku.terasology.procedural.FastRandom;
import com.gempukku.terasology.procedural.PDist;
import com.gempukku.terasology.world.WorldStorage;
import com.gempukku.terasology.world.chunk.ChunkBlocks;
import com.gempukku.terasology.world.chunk.ChunkSize;

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
    }

    @Override
    public void generateMeshForBlockFromAtlas(ChunkMeshGeneratorCallback callback,
                                              FloatArray vertices, ShortArray indices, Texture texture,
                                              ChunkBlocks chunkBlocks, int xInChunk, int yInChunk, int zInChunk) {
        init();

        WorldStorage.EntityRefAndCommonBlockId entityAndBlockId = worldStorage.getBlockEntityAndBlockIdAt(chunkBlocks.worldId,
                chunkBlocks.x * ChunkSize.X + xInChunk,
                chunkBlocks.y * ChunkSize.Y + yInChunk,
                chunkBlocks.z * ChunkSize.Z + zInChunk);

        BranchDefinition branchDefinition = createTreeDefinition(entityAndBlockId.entityRef);

        if (texture == oakBarkTexture.getTexture()) {
            int vertexIndex = (short) (vertices.size / 8);

            Matrix4 movingMatrix = new Matrix4().translate(
                    chunkBlocks.x * ChunkSize.X + xInChunk + 0.5f,
                    chunkBlocks.y * ChunkSize.Y + yInChunk,
                    chunkBlocks.z * ChunkSize.Z + zInChunk + 0.5f);

            BranchDrawingCallback branchCallback = new BranchDrawingCallback(vertexIndex, vertices, indices);

            generateTree(branchCallback, branchDefinition, movingMatrix);
        }
        if (texture == oakLeafTexture.getTexture()) {
            generateLeaves(branchDefinition, vertices, indices,
                    chunkBlocks.x * ChunkSize.X + xInChunk,
                    chunkBlocks.y * ChunkSize.Y + yInChunk,
                    chunkBlocks.z * ChunkSize.Z + zInChunk);
        }
    }

    private BranchDefinition createTreeDefinition(EntityRef entity) {
        int generation = 6;
        int seed = 0;
        FastRandom rnd = new FastRandom();
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

        float trunkRotation = rnd.nextFloat();

        BranchDefinition tree = new BranchDefinition(0, trunkRotation);
        for (int i = 0; i < generation; i++) {
            float lastBranchAngle = 0;
            // Grow existing segments and their branches
            for (BranchSegmentDefinition segment : tree.segments) {
                segment.length += 0.2f;
                segment.radius += 0.03f;
                for (BranchDefinition branch : segment.branches) {
                    lastBranchAngle += branch.rotationY;
                    for (BranchSegmentDefinition branchSegment : branch.segments) {
                        branchSegment.length += 0.08f;
                        branchSegment.radius += 0.01f;
                    }

                    branch.segments.add(new BranchSegmentDefinition(
                            branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0,
                            branchCurveAngleZ.getValue(rnd)));
                }
            }

            // Add new segment
            BranchSegmentDefinition segment = new BranchSegmentDefinition(
                    newTrunkSegmentLength.getValue(rnd), newTrunkSegmentRadius.getValue(rnd),
                    segmentRotateX.getValue(rnd), segmentRotateZ.getValue(rnd));

            int branchCount = rnd.nextInt(maxBranchesPerSegment) + 1;

            for (int branch = 0; branch < branchCount; branch++) {
                lastBranchAngle = lastBranchAngle + branchInitialAngleAddY.getValue(rnd);
                BranchDefinition branchDef = new BranchDefinition(
                        lastBranchAngle, branchInitialAngleZ.getValue(rnd));
                branchDef.segments.add(
                        new BranchSegmentDefinition(
                                branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0, 0));
                segment.branches.add(branchDef);
            }

            tree.segments.add(segment);
        }

        return tree;
    }

    private class BranchDrawingCallback implements LSystemCallback {
        private Vector3 origin = new Vector3();
        private Vector3 normal = new Vector3();

        private Vector3 first = new Vector3();
        private Vector3 second = new Vector3();
        private Vector3 third = new Vector3();
        private Vector3 fourth = new Vector3();

        private Vector3 firstTop = new Vector3();
        private Vector3 secondTop = new Vector3();
        private Vector3 thirdTop = new Vector3();
        private Vector3 fourthTop = new Vector3();

        private BranchDefinition lastFinishedSegmentBranch;

        private int vertexIndex;
        private FloatArray vertices;
        private ShortArray indices;

        public BranchDrawingCallback(int vertexIndex, FloatArray vertices, ShortArray indices) {
            this.vertexIndex = vertexIndex;
            this.vertices = vertices;
            this.indices = indices;
        }

        @Override
        public void branchStart(BranchDefinition branch, Matrix4 movingMatrix) {

        }

        @Override
        public void segmentStart(BranchSegmentDefinition segment, Matrix4 movingMatrix) {
            first.set(1, 0, 1).scl(segment.radius).mul(movingMatrix);
            second.set(-1, 0, 1).scl(segment.radius).mul(movingMatrix);
            third.set(-1, 0, -1).scl(segment.radius).mul(movingMatrix);
            fourth.set(1, 0, -1).scl(segment.radius).mul(movingMatrix);
        }

        @Override
        public void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4 movingMatrix) {
            float radius;
            if (nextSegment != null)
                radius = nextSegment.radius;
            else
                radius = 0;

            firstTop.set(1, 0, 1).scl(radius).mul(movingMatrix);
            secondTop.set(-1, 0, 1).scl(radius).mul(movingMatrix);
            thirdTop.set(-1, 0, -1).scl(radius).mul(movingMatrix);
            fourthTop.set(1, 0, -1).scl(radius).mul(movingMatrix);

            origin.set(0, 0, 0).mul(movingMatrix);

            normal.set(0, 0, 1).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    first, firstTop, secondTop, second);
            normal.set(-1, 0, 0).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    second, secondTop, thirdTop, third);
            normal.set(0, 0, -1).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    third, thirdTop, fourthTop, fourth);
            normal.set(1, 0, 0).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    fourth, fourthTop, firstTop, first);
        }

        @Override
        public void branchEnd(BranchDefinition branch, Matrix4 movingMatrix) {

        }
    }

    private void generateTree(LSystemCallback callback, BranchDefinition branchDefinition, Matrix4 movingMatrix) {
        movingMatrix.rotate(new Vector3(0, 1, 0), branchDefinition.rotationY);
        movingMatrix.rotate(new Vector3(0, 0, 1), branchDefinition.rotationZ);

        callback.branchStart(branchDefinition, movingMatrix);

        BranchSegmentDefinition segment;
        Iterator<BranchSegmentDefinition> segmentIterator = branchDefinition.segments.iterator();
        segment = segmentIterator.next();
        callback.segmentStart(segment, movingMatrix);

        while (segmentIterator.hasNext()) {
            BranchSegmentDefinition nextSegment = segmentIterator.next();

            movingMatrix.rotate(new Vector3(0, 0, 1), segment.rotateZ);
            movingMatrix.rotate(new Vector3(1, 0, 0), segment.rotateX);
            movingMatrix.translate(0, segment.length, 0);
            callback.segmentEnd(segment, nextSegment, movingMatrix);

            // Get back half of this segment to create branches
            movingMatrix.translate(0, -segment.length / 2, 0);

            for (BranchDefinition branch : segment.branches) {
                Matrix4 branchMatrix = movingMatrix.cpy();
                generateTree(callback, branch, branchMatrix);
            }
            movingMatrix.translate(0, segment.length / 2, 0);

            callback.segmentStart(nextSegment, movingMatrix);
            segment = nextSegment;
        }
        movingMatrix.translate(0, segment.length, 0);

        callback.segmentEnd(segment, null, movingMatrix);

        // Get back half of this segment to create branches
        movingMatrix.translate(0, -segment.length / 2, 0);

        for (BranchDefinition branch : segment.branches) {
            Matrix4 branchMatrix = movingMatrix.cpy();
            generateTree(callback, branch, branchMatrix);
        }
        movingMatrix.translate(0, segment.length / 2, 0);

        callback.branchEnd(branchDefinition, movingMatrix);
    }

    private int addQuad(int vertexIndex, FloatArray vertices, ShortArray indices,
                        Vector3 normal,
                        Vector3 first, Vector3 second, Vector3 third, Vector3 fourth) {
        vertices.add(first.x);
        vertices.add(first.y);
        vertices.add(first.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(oakBarkTexture.getU() + 1 * (oakBarkTexture.getU2() - oakBarkTexture.getU()));
        vertices.add(oakBarkTexture.getV() + 0 * (oakBarkTexture.getV2() - oakBarkTexture.getV()));

        vertices.add(second.x);
        vertices.add(second.y);
        vertices.add(second.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(oakBarkTexture.getU() + 1 * (oakBarkTexture.getU2() - oakBarkTexture.getU()));
        vertices.add(oakBarkTexture.getV() + 1 * (oakBarkTexture.getV2() - oakBarkTexture.getV()));

        vertices.add(third.x);
        vertices.add(third.y);
        vertices.add(third.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(oakBarkTexture.getU() + 0 * (oakBarkTexture.getU2() - oakBarkTexture.getU()));
        vertices.add(oakBarkTexture.getV() + 1 * (oakBarkTexture.getV2() - oakBarkTexture.getV()));

        vertices.add(fourth.x);
        vertices.add(fourth.y);
        vertices.add(fourth.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(oakBarkTexture.getU() + 0 * (oakBarkTexture.getU2() - oakBarkTexture.getU()));
        vertices.add(oakBarkTexture.getV() + 0 * (oakBarkTexture.getV2() - oakBarkTexture.getV()));

        indices.add(vertexIndex);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 3);
        indices.add(vertexIndex);

        return vertexIndex + 4;
    }

    private void generateLeaves(BranchDefinition branchDefinition, FloatArray vertices, ShortArray indices, int x, int y, int z) {
        // TODO
    }

    private void init() {
        if (oakBarkTexture == null) {
            oakBarkTexture = textureAtlasProvider.getTexture("blockTiles/plant/Tree/OakBark");
            oakLeafTexture = textureAtlasProvider.getTexture("blockTiles/plant/leaf/GreenLeaf");
        }
    }

    private interface LSystemCallback {
        void branchStart(BranchDefinition branch, Matrix4 movingMatrix);

        void segmentStart(BranchSegmentDefinition segment, Matrix4 movingMatrix);

        void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4 movingMatrix);

        void branchEnd(BranchDefinition branch, Matrix4 movingMatrix);
    }
}
