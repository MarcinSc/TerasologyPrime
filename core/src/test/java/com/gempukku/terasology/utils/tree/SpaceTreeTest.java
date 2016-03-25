package com.gempukku.terasology.utils.tree;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpaceTreeTest {
    private SpaceTree<Integer> tree = new SpaceTree(1, 1);

    @Test
    public void addMultiple() {
        tree.add(new float[]{1}, 1);
        tree.add(new float[]{2}, 2);
        tree.add(new float[]{3}, 3);
        tree.add(new float[]{4}, 4);

        DimensionalMap.Entry<Integer> nearest = tree.findNearest(new float[]{3.4f}, 0.6f);
        assertEquals(3, nearest.value.intValue());
    }

    @Test
    public void addAndRemoveMultiple() {
        for (int i = 0; i < 20; i++) {
            tree.add(new float[]{i}, i);
        }

        tree.remove(new float[]{5});

        assertEquals(6, tree.findNearest(new float[]{5.3f}).value.intValue());
    }
}