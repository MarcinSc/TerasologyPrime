package com.gempukku.terasology.trees.leaves;

public interface LeavesGeneratorRegistry {
    void registerLeavesGenerator(String generatorName, LeavesGenerator leavesGenerator);
}
