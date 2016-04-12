package com.gempukku.terasology.trees;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.graphics.environment.mesh.ChunkMeshGenerator;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.procedural.FastRandom;
import com.gempukku.terasology.procedural.PDist;
import com.gempukku.terasology.trees.component.IndividualTreeComponent;
import com.gempukku.terasology.trees.component.SimpleTreeDefinitionComponent;
import com.gempukku.terasology.trees.model.BranchDefinition;
import com.gempukku.terasology.trees.model.BranchSegmentDefinition;
import com.gempukku.terasology.trees.model.TreeDefinition;
import com.gempukku.terasology.world.component.SeedComponent;

import java.util.HashSet;
import java.util.Set;

@RegisterSystem(
        profiles = "generateChunkGeometry")
public class SimpleTreeGenerator implements TreeGenerator, LifeCycleSystem {
    @In
    private ShapeProvider shapeProvider;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private TreeGeneratorRegistry treeGeneratorRegistry;
    @In
    private PrefabManager prefabManager;

    @Override
    public void initialize() {
        treeGeneratorRegistry.registerTreeGenerator("simple", this);

        Set<String> texturesToLoad = new HashSet<>();
        for (EntityData entityData : prefabManager.findPrefabsWithComponents(SimpleTreeDefinitionComponent.class)) {
            ComponentData component = entityData.getComponent(SimpleTreeDefinitionComponent.class);
            texturesToLoad.add((String) component.getFields().get("barkTexture"));
        }
        textureAtlasRegistry.registerTextures(ChunkMeshGenerator.CHUNK_ATLAS_NAME, texturesToLoad);
    }

    @Override
    public TreeDefinition generateTreeDefinition(EntityRef entityRef) {
        SimpleTreeDefinitionComponent simpleTreeDefinition = entityRef.getComponent(SimpleTreeDefinitionComponent.class);
        IndividualTreeComponent individualTree = entityRef.getComponent(IndividualTreeComponent.class);
        SeedComponent seedComponent = entityRef.getComponent(SeedComponent.class);

        int generation = individualTree.getGeneration();

        FastRandom rnd = new FastRandom(seedComponent.getSeed());

        PDist trunkRotationDist = extractDist(simpleTreeDefinition.getInitialTrunkRotationDist());

        PDist newTrunkSegmentLength = extractDist(simpleTreeDefinition.getTrunkSegmentLengthDist());
        PDist newTrunkSegmentRadius = extractDist(simpleTreeDefinition.getTrunkSegmentRadiusDist());

        PDist segmentRotateX = extractDist(simpleTreeDefinition.getTrunkSegmentRotateXDist());
        PDist segmentRotateZ = extractDist(simpleTreeDefinition.getTrunkSegmentRotateZDist());

        PDist segmentLengthIncreasePerGeneration = extractDist(simpleTreeDefinition.getTrunkSegmentLengthIncreasePerGenerationDist());
        PDist segmentRadiusIncreasePerGeneration = extractDist(simpleTreeDefinition.getTrunkSegmentRadiusIncreasePerGenerationDist());

        PDist branchCountDist = extractDist(simpleTreeDefinition.getBranchCountDist());

        PDist branchInitialLength = extractDist(simpleTreeDefinition.getBranchLengthDist());
        PDist branchInitialRadius = extractDist(simpleTreeDefinition.getBranchRadiusDist());

        PDist branchInitialAngleAddY = extractDist(simpleTreeDefinition.getBranchInitialAngleAddYDist());
        PDist branchInitialAngleZ = extractDist(simpleTreeDefinition.getBranchInitialAngleZDist());

        PDist branchCurveAngleZ = extractDist(simpleTreeDefinition.getBranchCurveAngleZDist());

        PDist branchSegmentLengthIncreasePerGeneration = extractDist(simpleTreeDefinition.getBranchSegmentLengthIncreasePerGenerationDist());
        PDist branchSegmentRadiusIncreasePerGeneration = extractDist(simpleTreeDefinition.getBranchSegmentRadiusIncreasePerGenerationDist());

        BranchDefinition tree = new BranchDefinition(0, trunkRotationDist.getValue(rnd));
        for (int i = 0; i < generation; i++) {
            float lastBranchAngle = 0;

            // Grow existing segments and their branches
            for (BranchSegmentDefinition segment : tree.segments) {
                segment.length += segmentLengthIncreasePerGeneration.getValue(rnd);
                segment.radius += segmentRadiusIncreasePerGeneration.getValue(rnd);
                for (BranchDefinition branch : segment.branches) {
                    lastBranchAngle += branch.rotationY;
                    for (BranchSegmentDefinition branchSegment : branch.segments) {
                        branchSegment.length += branchSegmentLengthIncreasePerGeneration.getValue(rnd);
                        branchSegment.radius += branchSegmentRadiusIncreasePerGeneration.getValue(rnd);
                    }

                    branch.segments.add(new BranchSegmentDefinition(
                            branchInitialLength.getValue(rnd), branchInitialRadius.getValue(rnd), 0,
                            branchCurveAngleZ.getValue(rnd)));
                }
            }

            int existingSegmentCount = tree.segments.size();

            if (existingSegmentCount > 2) {
                // Add branches to last existing segment
                int branchCount = branchCountDist.getIntValue(rnd);

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

        return new TreeDefinition(tree,
                textureAtlasProvider.getTexture(ChunkMeshGenerator.CHUNK_ATLAS_NAME, simpleTreeDefinition.getBarkTexture()),
                simpleTreeDefinition.getLeavesGenerator());
    }

    private PDist extractDist(String value) {
        PDist.Type type;

        String[] split = value.split(",");
        if (split[0].equals("u")) {
            type = PDist.Type.uniform;
        } else if (split[0].equals("n")) {
            type = PDist.Type.normal;
        } else {
            throw new IllegalArgumentException("Unknown PDist type: " + split[0]);
        }
        float mean = Float.parseFloat(split[1]);
        float range = Float.parseFloat(split[2]);
        return new PDist(mean, range, type);
    }
}
