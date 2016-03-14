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
import java.util.LinkedList;
import java.util.List;

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

        TreeDefinition treeDefinition = createTreeDefinition(entityAndBlockId.entityRef);

        if (texture == oakBarkTexture.getTexture()) {
            generateTrunk(treeDefinition, vertices, indices,
                    chunkBlocks.x * ChunkSize.X + xInChunk,
                    chunkBlocks.y * ChunkSize.Y + yInChunk,
                    chunkBlocks.z * ChunkSize.Z + zInChunk);
        }
        if (texture == oakLeafTexture.getTexture()) {
            generateLeaves(treeDefinition, vertices, indices,
                    chunkBlocks.x * ChunkSize.X + xInChunk,
                    chunkBlocks.y * ChunkSize.Y + yInChunk,
                    chunkBlocks.z * ChunkSize.Z + zInChunk);
        }
    }

    private TreeDefinition createTreeDefinition(EntityRef entity) {
        int generation = 6;
        int seed = 0;
        FastRandom rnd = new FastRandom();
        PDist newTrunkSegmentLength = new PDist(0.8f, 0.2f, PDist.Type.normal);
        PDist newTrunkSegmentRadius = new PDist(0.02f, 0.005f, PDist.Type.normal);

        int maxBranchesPerSegment = 2;
        PDist branchInitialAngleAddY = new PDist(120f, 10f, PDist.Type.normal);
        PDist branchInitialAngleZ = new PDist(60, 20, PDist.Type.normal);
        PDist branchInitialLength = new PDist(0.3f, 0.075f, PDist.Type.normal);
        PDist branchInitialRadius = new PDist(0.01f, 0.003f, PDist.Type.normal);

        PDist segmentRotateX = new PDist(0, 15, PDist.Type.normal);
        PDist segmentRotateZ = new PDist(0, 15, PDist.Type.normal);

        PDist branchCurveAngleZ = new PDist(-8, 2, PDist.Type.normal);

        float trunkRotation = rnd.nextFloat();

        TreeDefinition tree = new TreeDefinition(trunkRotation);
        for (int i = 0; i < generation; i++) {
            float lastBranchAngle = 0;
            // Grow existing segments and their branches
            for (TrunkSegmentDefinition segment : tree.segments) {
                segment.length += 0.2f;
                segment.radius += 0.03f;
                for (BranchDefinition branch : segment.branches) {
                    lastBranchAngle += branch.angleY;
                    for (BranchSegmentDefinition branchSegment : branch.branchSegments) {
                        branchSegment.length += 0.08f;
                        branchSegment.radius += 0.01f;
                    }

                    branch.branchSegments.add(new BranchSegmentDefinition(
                            branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), branchCurveAngleZ.getValue(rnd)));
                }
            }

            int branchCount = rnd.nextInt(maxBranchesPerSegment) + 1;

            List<BranchDefinition> branches = new LinkedList<>();
            for (int branch = 0; branch < branchCount; branch++) {
                lastBranchAngle = lastBranchAngle + branchInitialAngleAddY.getValue(rnd);
                BranchDefinition branchDef = new BranchDefinition(
                        lastBranchAngle, branchInitialAngleZ.getValue(rnd));
                branchDef.branchSegments.add(
                        new BranchSegmentDefinition(
                                branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0));
                branches.add(branchDef);
            }

            // Add new segment
            TrunkSegmentDefinition segment = new TrunkSegmentDefinition(branches,
                    newTrunkSegmentLength.getValue(rnd), newTrunkSegmentRadius.getValue(rnd),
                    segmentRotateX.getValue(rnd), segmentRotateZ.getValue(rnd));
            tree.addSegment(segment);
        }

        return tree;
    }

    private void generateTrunk(TreeDefinition treeDefinition, FloatArray vertices, ShortArray indices, int x, int y, int z) {
        Matrix4 movingMatrix = new Matrix4().translate(x + 0.5f, y, z + 0.5f);
        movingMatrix.rotate(new Vector3(0, 1, 0), treeDefinition.trunkRotationY);

        int vertexIndex = (short) (vertices.size / 8);

        Vector3 origin = new Vector3();
        Vector3 normal = new Vector3();

        Iterator<TrunkSegmentDefinition> segmentIterator = treeDefinition.segments.iterator();
        TrunkSegmentDefinition lastSegment = segmentIterator.next();
        Vector3 first = new Vector3(1, 0, 1).scl(lastSegment.radius).mul(movingMatrix);
        Vector3 second = new Vector3(-1, 0, 1).scl(lastSegment.radius).mul(movingMatrix);
        Vector3 third = new Vector3(-1, 0, -1).scl(lastSegment.radius).mul(movingMatrix);
        Vector3 fourth = new Vector3(1, 0, -1).scl(lastSegment.radius).mul(movingMatrix);
        while (segmentIterator.hasNext()) {
            TrunkSegmentDefinition thisSegment = segmentIterator.next();

            movingMatrix.rotate(new Vector3(0, 0, 1), thisSegment.rotateZ);
            movingMatrix.rotate(new Vector3(1, 0, 0), thisSegment.rotateX);
            movingMatrix.translate(0, lastSegment.length / 2, 0);
            for (BranchDefinition branch : lastSegment.branches) {
                Matrix4 branchMatrix = movingMatrix.cpy();
                vertexIndex = generateBranch(vertexIndex, branch, branchMatrix, vertices, indices);
            }

            movingMatrix.translate(0, lastSegment.length / 2, 0);

            origin.set(0, 0, 0).mul(movingMatrix);

            Vector3 firstTop = new Vector3(1, 0, 1).scl(thisSegment.radius).mul(movingMatrix);
            Vector3 secondTop = new Vector3(-1, 0, 1).scl(thisSegment.radius).mul(movingMatrix);
            Vector3 thirdTop = new Vector3(-1, 0, -1).scl(thisSegment.radius).mul(movingMatrix);
            Vector3 fourthTop = new Vector3(1, 0, -1).scl(thisSegment.radius).mul(movingMatrix);

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

            first.set(firstTop);
            second.set(secondTop);
            third.set(thirdTop);
            fourth.set(fourthTop);

            lastSegment = thisSegment;
        }

        movingMatrix.translate(0, lastSegment.length, 0);

        origin.set(0, 0, 0).mul(movingMatrix);

        Vector3 firstTop = new Vector3(0, 0, 0).mul(movingMatrix);
        Vector3 secondTop = new Vector3(0, 0, 0).mul(movingMatrix);
        Vector3 thirdTop = new Vector3(0, 0, 0).mul(movingMatrix);
        Vector3 fourthTop = new Vector3(0, 0, 0).mul(movingMatrix);

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

    private int generateBranch(int vertexIndex, BranchDefinition branch, Matrix4 movingMatrix, FloatArray vertices, ShortArray indices) {
        movingMatrix.rotate(new Vector3(0, 1, 0), branch.angleY);
        movingMatrix.rotate(new Vector3(0, 0, 1), branch.angleZ);

        Vector3 origin = new Vector3();
        Vector3 normal = new Vector3();

        Iterator<BranchSegmentDefinition> segmentIterator = branch.branchSegments.iterator();
        BranchSegmentDefinition lastSegment = segmentIterator.next();
        Vector3 first = new Vector3(1, 0, 1).scl(lastSegment.radius).mul(movingMatrix);
        Vector3 second = new Vector3(-1, 0, 1).scl(lastSegment.radius).mul(movingMatrix);
        Vector3 third = new Vector3(-1, 0, -1).scl(lastSegment.radius).mul(movingMatrix);
        Vector3 fourth = new Vector3(1, 0, -1).scl(lastSegment.radius).mul(movingMatrix);
        while (segmentIterator.hasNext()) {
            BranchSegmentDefinition thisSegment = segmentIterator.next();

            movingMatrix.rotate(new Vector3(0, 0, 1), thisSegment.rotateZ);
            movingMatrix.translate(0, lastSegment.length, 0);

            Vector3 firstTop = new Vector3(1, 0, 1).scl(thisSegment.radius).mul(movingMatrix);
            Vector3 secondTop = new Vector3(-1, 0, 1).scl(thisSegment.radius).mul(movingMatrix);
            Vector3 thirdTop = new Vector3(-1, 0, -1).scl(thisSegment.radius).mul(movingMatrix);
            Vector3 fourthTop = new Vector3(1, 0, -1).scl(thisSegment.radius).mul(movingMatrix);

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

            first.set(firstTop);
            second.set(secondTop);
            third.set(thirdTop);
            fourth.set(fourthTop);

            lastSegment = thisSegment;
        }

        movingMatrix.translate(0, lastSegment.length, 0);

        origin.set(0, 0, 0).mul(movingMatrix);

        Vector3 firstTop = new Vector3(0, 0, 0).mul(movingMatrix);
        Vector3 secondTop = new Vector3(0, 0, 0).mul(movingMatrix);
        Vector3 thirdTop = new Vector3(0, 0, 0).mul(movingMatrix);
        Vector3 fourthTop = new Vector3(0, 0, 0).mul(movingMatrix);

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

        return vertexIndex;
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

    private void generateLeaves(TreeDefinition treeDefinition, FloatArray vertices, ShortArray indices, int x, int y, int z) {
        // TODO
    }

    private void init() {
        if (oakBarkTexture == null) {
            oakBarkTexture = textureAtlasProvider.getTexture("blockTiles/plant/Tree/OakBark");
            oakLeafTexture = textureAtlasProvider.getTexture("blockTiles/plant/leaf/GreenLeaf");
        }
    }
}
