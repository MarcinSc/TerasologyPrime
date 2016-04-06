package com.gempukku.terasology.particle.selector;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ParticleAgeCombinedTextureSelector extends ParticleAgeTextureSelector {
    public ParticleAgeCombinedTextureSelector(TextureRegion texture, int rows, int columns) {
        super(convert(texture, rows, columns));
    }

    private static TextureRegion[] convert(TextureRegion texture, int rows, int columns) {
        TextureRegion[] result = new TextureRegion[rows * columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result[i * columns + j] = new TextureRegion(texture.getTexture(),
                        texture.getU() + (texture.getU2() - texture.getU()) / columns * j,
                        texture.getV() + (texture.getV2() - texture.getV()) / rows * i,
                        texture.getU() + (texture.getU2() - texture.getU()) / columns * (j + 1),
                        texture.getV() + (texture.getV2() - texture.getV()) / rows * (i + 1));
            }
        }
        return result;
    }
}
