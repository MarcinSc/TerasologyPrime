package com.gempukku.terasology.world.chunk.geometry;

public interface BlockGeometryGeneratorRegistry {
    void registerBlockMeshGenerator(String generatorType, BlockGeometryGenerator generator);
}
