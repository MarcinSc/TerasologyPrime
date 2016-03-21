package com.gempukku.terasology.trees.component;

import com.gempukku.secsy.entity.Component;
import com.gempukku.terasology.communication.SharedComponent;

@SharedComponent
public interface SimpleTreeDefinitionComponent extends Component {
    String getBarkTexture();

    String getLeavesGenerator();

    int getMaxGenerations();

    ////////////////////////////////////////////////
    /////////////// Trunk generation ///////////////
    ////////////////////////////////////////////////

    /**
     * Describes the initial rotation around Y-axis of the tree.
     *
     * @return
     */
    String getInitialTrunkRotationDist();

    /**
     * Describes the length of trunk segment as it is being created.
     *
     * @return
     */
    String getTrunkSegmentLengthDist();

    /**
     * Describes the radius of trunk segment as it is being created.
     *
     * @return
     */
    String getTrunkSegmentRadiusDist();

    /**
     * Describes the rotation around X-axis of each subsequent (non-first) trunk segment.
     *
     * @return
     */
    String getTrunkSegmentRotateXDist();

    /**
     * Describes the rotation around z-axis of each subsequent (non-first) trunk segment.
     *
     * @return
     */
    String getTrunkSegmentRotateZDist();

    /**
     * Describes the length increase per generation of each existing trunk segment.
     *
     * @return
     */
    String getTrunkSegmentLengthIncreasePerGenerationDist();

    /**
     * Describes the radius increase per generation of each existing trunk segment.
     *
     * @return
     */
    String getTrunkSegmentRadiusIncreasePerGenerationDist();

    ////////////////////////////////////////////////
    /////////////// Branch generation //////////////
    ////////////////////////////////////////////////

    /**
     * Describes the count of branches being generated for each new segment (except the bottom 2).
     *
     * @return
     */
    String getBranchCountDist();

    /**
     * Describes the length of branch segment as it is being created.
     *
     * @return
     */
    String getBranchLengthDist();

    /**
     * Describes the radius of branch segment as it is being created.
     *
     * @return
     */
    String getBranchRadiusDist();

    /**
     * Describes the initial rotation of branch around the Y-axis. The angle returned is added to the
     * last generated branch angle for that tree.
     * Note that for a lot of trees in nature - consecutive branches grow at 137.5 degrees relative to each other,
     * this is known as a golden angle, which guarantees the best light distribution to leaves of a tree.
     *
     * @return
     */
    String getBranchInitialAngleAddYDist();

    /**
     * Describes the initial rotation of branch around the Z-axis.
     *
     * @return
     */
    String getBranchInitialAngleZDist();

    /**
     * Descibes the Z-axis rotation of each new (non-first) branch segment.
     *
     * @return
     */
    String getBranchCurveAngleZDist();

    /**
     * Describes the length increase per generation of each existing branch segment.
     *
     * @return
     */
    String getBranchSegmentLengthIncreasePerGenerationDist();

    /**
     * Describes the radius increase per generation of each existing branch segment.
     *
     * @return
     */
    String getBranchSegmentRadiusIncreasePerGenerationDist();
}
