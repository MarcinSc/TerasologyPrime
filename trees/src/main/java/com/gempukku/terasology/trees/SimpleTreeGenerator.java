package com.gempukku.terasology.trees;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.procedural.FastRandom;
import com.gempukku.terasology.procedural.PDist;

import java.util.Arrays;

@RegisterSystem(
        profiles = "generateChunkMeshes")
public class SimpleTreeGenerator implements TreeGenerator, LifeCycleSystem {
    @In
    private ShapeProvider shapeProvider;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private TreeGenerationRegistry treeGenerationRegistry;

    @Override
    public void initialize() {
        textureAtlasRegistry.registerTextures(
                Arrays.asList(
                        "blockTiles/trees/OakBark.png",
                        "blockTiles/plant/leaf/GreenLeaf.png"));
        treeGenerationRegistry.registerTreeGenerator("simple", this);
    }

    @Override
    public TreeDefinition generateTreeDefinition(EntityRef entityRef) {
        FastRandom rnd = new FastRandom(0);

        int generation = 5;

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

        return new TreeDefinition(shapeProvider.getShapeById("cube"),
                textureAtlasProvider.getTexture("blockTiles/trees/OakBark.png"),
                textureAtlasProvider.getTexture("blockTiles/plant/leaf/GreenLeaf.png"), tree);

    }
}
