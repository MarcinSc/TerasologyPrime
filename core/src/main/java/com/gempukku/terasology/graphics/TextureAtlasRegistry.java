package com.gempukku.terasology.graphics;

import java.util.Collection;

public interface TextureAtlasRegistry {
    void registerTextures(String textureAtlasId, Collection<String> textures);
}
