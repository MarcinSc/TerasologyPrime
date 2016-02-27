package com.gempukku.terasology.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.prefab.PrefabData;
import com.gempukku.terasology.prefab.PrefabManager;
import com.gempukku.terasology.world.component.CommonBlockComponent;
import com.gempukku.terasology.world.component.TextureComponent;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = TextureAtlasProvider.class)
public class ReflectionsTextureAtlasProvider implements TextureAtlasProvider, LifeCycleSystem {
    @In
    private PrefabManager prefabManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;

    private TextureAtlas textureAtlas;
    private Map<String, TextureRegion> textures = new HashMap<>();

    @Override
    public void initialize() {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 512;
        settings.maxHeight = 512;

        File resourceRoot = new File(ReflectionsTextureAtlasProvider.class.getResource("/badlogic.jpg").getPath()).getParentFile();
        TexturePacker texturePacker = new TexturePacker(resourceRoot, settings);

        for (PrefabData prefabData : prefabManager.findPrefabsWithComponents(CommonBlockComponent.class, TextureComponent.class)) {
            String textureComponentName = terasologyComponentManager.getNameByComponent(TextureComponent.class);
            for (String partTexture : ((Map<String, String>) prefabData.getComponents().get(textureComponentName).getFields().get("parts")).values()) {
                URL textureResource = ReflectionsTextureAtlasProvider.class.getResource("/" + partTexture);
                if (textureResource != null) {
                    File imageFile = new File(textureResource.getPath());
                    texturePacker.addImage(imageFile);
                }
            }
        }

        FileHandle temp = Gdx.files.local("temp");
        if (temp.exists()) {
            File atlasLocation = temp.file();

            for (File file : atlasLocation.listFiles()) {
                file.delete();
            }
        }

        FileHandle atlasFileHandle = Gdx.files.local("temp/blocks.atlas");

        texturePacker.pack(temp.file(), "blocks");

        textureAtlas = new TextureAtlas(atlasFileHandle);

        for (TextureAtlas.AtlasRegion atlasRegion : textureAtlas.getRegions()) {
            textures.put(atlasRegion.name, atlasRegion);
        }
    }

    @Override
    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }

    @Override
    public TextureRegion getTexture(String name) {
        return textures.get(name);
    }
}
