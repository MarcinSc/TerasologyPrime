package com.gempukku.terasology.trees;

public interface LeavesGeneratorRegistry {
    void registerLeavesGenerator(String generatorName, LeavesGenerator leavesGenerator);
}
