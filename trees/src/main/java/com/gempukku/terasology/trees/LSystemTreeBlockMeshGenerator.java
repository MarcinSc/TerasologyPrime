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
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
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
    @In
    private ShapeProvider shapeProvider;

    private TextureRegion oakBarkTexture;
    private TextureRegion oakLeafTexture;
    private ShapeDef cubeShape;

    @Override
    public void initialize() {
        textureAtlasRegistry.registerTextures(
                Arrays.asList(
                        "blockTiles/trees/OakBark.png",
                        "blockTiles/plant/leaf/GreenLeaf.png"));
        blockMeshGeneratorRegistry.registerBlockMeshGenerator("trees:tree", this);
        cubeShape = shapeProvider.getShapeById("cube");
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
            short vertexIndex = (short) (vertices.size / 8);

            Matrix4 movingMatrix = new Matrix4().translate(
                    chunkBlocks.x * ChunkSize.X + xInChunk + 0.5f,
                    chunkBlocks.y * ChunkSize.Y + yInChunk,
                    chunkBlocks.z * ChunkSize.Z + zInChunk + 0.5f);

            BranchDrawingCallback branchCallback = new BranchDrawingCallback(vertexIndex, vertices, indices);

            processBranchWithCallback(branchCallback, branchDefinition, movingMatrix);
        }
        if (texture == oakLeafTexture.getTexture()) {
            short vertexIndex = (short) (vertices.size / 8);

            Matrix4 movingMatrix = new Matrix4().translate(
                    chunkBlocks.x * ChunkSize.X + xInChunk + 0.5f,
                    chunkBlocks.y * ChunkSize.Y + yInChunk,
                    chunkBlocks.z * ChunkSize.Z + zInChunk + 0.5f);

            LeavesDrawingCallback branchCallback = new LeavesDrawingCallback(vertexIndex, vertices, indices);

            processBranchWithCallback(branchCallback, branchDefinition, movingMatrix);
        }
    }

    private BranchDefinition createTreeDefinition(EntityRef entity) {
        int generation = 10;
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

        BranchDefinition tree = new BranchDefinition(0, trunkRotation, 0.2f, 0.15f);
        for (int i = 0; i < generation; i++) {
            tree.horizontalLeavesScale = (float) Math.pow(tree.segments.size(), 2) / 10f;
            tree.verticalLeavesScale = tree.horizontalLeavesScale * 0.75f;

            float lastBranchAngle = 0;

            // Grow existing segments and their branches
            for (BranchSegmentDefinition segment : tree.segments) {
                segment.length += 0.2f;
                segment.radius += 0.05f;
                for (BranchDefinition branch : segment.branches) {
                    branch.horizontalLeavesScale = (float) Math.pow(branch.segments.size(), 2) / 10f;
                    branch.verticalLeavesScale = branch.horizontalLeavesScale * 0.75f;

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

            int existingSegmentCount = tree.segments.size();

            if (existingSegmentCount > 2) {
                // Add branches to last existing segment
                int branchCount = rnd.nextInt(maxBranchesPerSegment) + 1;

                for (int branch = 0; branch < branchCount; branch++) {
                    lastBranchAngle = lastBranchAngle + branchInitialAngleAddY.getValue(rnd);
                    BranchDefinition branchDef = new BranchDefinition(
                            lastBranchAngle, branchInitialAngleZ.getValue(rnd), 0.2f, 0.15f);
                    branchDef.segments.add(
                            new BranchSegmentDefinition(
                                    branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0, 0));
                    tree.segments.get(existingSegmentCount - 1).branches.add(branchDef);
                }
            }

            // Add new segment
            BranchSegmentDefinition segment = new BranchSegmentDefinition(
                    newTrunkSegmentLength.getValue(rnd), newTrunkSegmentRadius.getValue(rnd),
                    segmentRotateX.getValue(rnd), segmentRotateZ.getValue(rnd));

            tree.segments.add(segment);
        }

        return tree;
    }

    private class LeavesDrawingCallback implements LSystemCallback {
        private short vertexIndex;
        private FloatArray vertices;
        private ShortArray indices;

        private Vector3 tempVector = new Vector3();
        private Vector3 origin = new Vector3();

        public LeavesDrawingCallback(short vertexIndex, FloatArray vertices, ShortArray indices) {
            this.vertexIndex = vertexIndex;
            this.vertices = vertices;
            this.indices = indices;
        }

        @Override
        public void branchStart(BranchDefinition branch, Matrix4 movingMatrix) {

        }

        @Override
        public void segmentStart(BranchSegmentDefinition segment, Matrix4 movingMatrix) {

        }

        @Override
        public void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4 movingMatrix) {

        }

        @Override
        public void branchEnd(BranchDefinition branch, Matrix4 movingMatrix) {
            origin.set(0, 0, 0).mul(movingMatrix);

            for (ShapePartDef shapePart : cubeShape.getShapeParts()) {
                int vertexCount = shapePart.getVertices().size();

                // This array will store indexes of vertices in the resulting Mesh
                short[] vertexMapping = new short[vertexCount];

                // Trunk
                for (int vertex = 0; vertex < vertexCount; vertex++) {
                    vertexMapping[vertex] = vertexIndex++;

                    Float[] vertexCoords = shapePart.getVertices().get(vertex);
                    Float[] normalValues = shapePart.getNormals().get(vertex);
                    Float[] textureCoords = shapePart.getUvs().get(vertex);

                    tempVector.set(vertexCoords[0] - 0.5f, vertexCoords[1] - 1f, vertexCoords[2] - 0.5f)
                            .scl(branch.horizontalLeavesScale, branch.verticalLeavesScale, branch.horizontalLeavesScale).add(origin);

                    vertices.add(tempVector.x);
                    vertices.add(tempVector.y);
                    vertices.add(tempVector.z);

                    tempVector.set(normalValues[0], normalValues[1], normalValues[2]).mul(movingMatrix).sub(origin);

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

        private short vertexIndex;
        private FloatArray vertices;
        private ShortArray indices;

        public BranchDrawingCallback(short vertexIndex, FloatArray vertices, ShortArray indices) {
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
                    first, firstTop, secondTop, second, oakBarkTexture);
            normal.set(-1, 0, 0).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    second, secondTop, thirdTop, third, oakBarkTexture);
            normal.set(0, 0, -1).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    third, thirdTop, fourthTop, fourth, oakBarkTexture);
            normal.set(1, 0, 0).mul(movingMatrix).sub(origin);
            vertexIndex = addQuad(vertexIndex, vertices, indices, normal,
                    fourth, fourthTop, firstTop, first, oakBarkTexture);
        }

        @Override
        public void branchEnd(BranchDefinition branch, Matrix4 movingMatrix) {

        }
    }

    private void processBranchWithCallback(LSystemCallback callback, BranchDefinition branchDefinition, Matrix4 movingMatrix) {
        movingMatrix.rotate(new Vector3(0, 1, 0), branchDefinition.rotationY);
        movingMatrix.rotate(new Vector3(0, 0, 1), branchDefinition.rotationZ);

        callback.branchStart(branchDefinition, movingMatrix);

        Iterator<BranchSegmentDefinition> segmentIterator = branchDefinition.segments.iterator();
        BranchSegmentDefinition currentSegment = segmentIterator.next();
        do {
            BranchSegmentDefinition nextSegment = segmentIterator.hasNext() ? segmentIterator.next() : null;

            callback.segmentStart(currentSegment, movingMatrix);
            movingMatrix.rotate(new Vector3(0, 0, 1), currentSegment.rotateZ);
            movingMatrix.rotate(new Vector3(1, 0, 0), currentSegment.rotateX);
            movingMatrix.translate(0, currentSegment.length, 0);
            callback.segmentEnd(currentSegment, nextSegment, movingMatrix);

            // Get back half of the segment to create branches
            movingMatrix.translate(0, -currentSegment.length / 2, 0);

            for (BranchDefinition branch : currentSegment.branches) {
                Matrix4 branchMatrix = movingMatrix.cpy();
                processBranchWithCallback(callback, branch, branchMatrix);
            }
            movingMatrix.translate(0, currentSegment.length / 2, 0);

            currentSegment = nextSegment;
        } while (currentSegment != null);

        callback.branchEnd(branchDefinition, movingMatrix);
    }

    private short addQuad(short vertexIndex, FloatArray vertices, ShortArray indices,
                          Vector3 normal,
                          Vector3 first, Vector3 second, Vector3 third, Vector3 fourth, TextureRegion texture) {
        vertices.add(first.x);
        vertices.add(first.y);
        vertices.add(first.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(texture.getU() + 1 * (texture.getU2() - texture.getU()));
        vertices.add(texture.getV() + 0 * (texture.getV2() - texture.getV()));

        vertices.add(second.x);
        vertices.add(second.y);
        vertices.add(second.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(texture.getU() + 1 * (texture.getU2() - texture.getU()));
        vertices.add(texture.getV() + 1 * (texture.getV2() - texture.getV()));

        vertices.add(third.x);
        vertices.add(third.y);
        vertices.add(third.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(texture.getU() + 0 * (texture.getU2() - texture.getU()));
        vertices.add(texture.getV() + 1 * (texture.getV2() - texture.getV()));

        vertices.add(fourth.x);
        vertices.add(fourth.y);
        vertices.add(fourth.z);
        vertices.add(normal.x);
        vertices.add(normal.y);
        vertices.add(normal.z);
        vertices.add(texture.getU() + 0 * (texture.getU2() - texture.getU()));
        vertices.add(texture.getV() + 0 * (texture.getV2() - texture.getV()));

        indices.add(vertexIndex);
        indices.add(vertexIndex + 1);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 2);
        indices.add(vertexIndex + 3);
        indices.add(vertexIndex);

        return (short) (vertexIndex + 4);
    }

    private void init() {
        if (oakBarkTexture == null) {
            oakBarkTexture = textureAtlasProvider.getTexture("trees/blockTiles/trees/OakBark");
            oakLeafTexture = textureAtlasProvider.getTexture("core/blockTiles/plant/leaf/GreenLeaf");
        }
    }

    private interface LSystemCallback {
        void branchStart(BranchDefinition branch, Matrix4 movingMatrix);

        void segmentStart(BranchSegmentDefinition segment, Matrix4 movingMatrix);

        void segmentEnd(BranchSegmentDefinition segment, BranchSegmentDefinition nextSegment, Matrix4 movingMatrix);

        void branchEnd(BranchDefinition branch, Matrix4 movingMatrix);
    }
}
