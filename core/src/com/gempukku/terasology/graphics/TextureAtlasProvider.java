package com.gempukku.terasology.graphics;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public interface TextureAtlasProvider {
    TextureAtlas getTextureAtlas();
    TextureRegion getTexture(String name);
}
