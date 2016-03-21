package com.gempukku.terasology.trees;

public interface TreeGeneratorRegistry {
    void registerTreeGenerator(String generatorName, TreeGenerator treeGenerator);
}
