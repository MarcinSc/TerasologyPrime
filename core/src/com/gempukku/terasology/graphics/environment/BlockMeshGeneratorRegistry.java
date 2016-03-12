package com.gempukku.terasology.graphics.environment;

public interface BlockMeshGeneratorRegistry {
    void registerBlockMeshGenerator(String generatorType, BlockMeshGenerator generator);
}
