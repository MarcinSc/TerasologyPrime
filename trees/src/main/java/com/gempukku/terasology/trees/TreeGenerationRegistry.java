package com.gempukku.terasology.trees;

public interface TreeGenerationRegistry {
    void registerTreeGenerator(String generatorName, TreeGenerator treeGenerator);
}
