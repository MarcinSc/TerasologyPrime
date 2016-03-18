package com.gempukku.terasology.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.gempukku.terasology.prefab.PrefabManager;
import com.google.common.collect.Iterables;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = {TextureAtlasProvider.class, TextureAtlasRegistry.class})
public class ReflectionsTextureAtlasProvider implements TextureAtlasProvider, TextureAtlasRegistry, LifeCycleSystem {
    @In
    private PrefabManager prefabManager;
    @In
    private TerasologyComponentManager terasologyComponentManager;

    private TextureAtlas textureAtlas;
    private List<Texture> textureList;
    private Map<String, TextureRegion> textures = new HashMap<>();

    private Set<String> texturesToRegister = new HashSet<>();

    @Override
    public void registerTextures(Collection<String> textures) {
        texturesToRegister.addAll(textures);
    }

    @Override
    public void postInitialize() {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 512;
        settings.maxHeight = 512;
        settings.silent = true;

        File resourceRoot = new File(ReflectionsTextureAtlasProvider.class.getResource("/badlogic.jpg").getPath()).getParentFile().getParentFile();
        TexturePacker texturePacker = new TexturePacker(resourceRoot, settings);

        for (String texturePath : texturesToRegister) {
            URL textureResource = ReflectionsTextureAtlasProvider.class.getResource("/" + texturePath);
            texturePacker.addImage(new File(textureResource.getPath()));
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
            String name = atlasRegion.name;
            textures.put(name.substring(name.indexOf('/') + 1), atlasRegion);
        }

        textureList = new ArrayList<>();
        Iterables.addAll(textureList, textureAtlas.getTextures());

        texturesToRegister = null;
    }

    @Override
    public List<Texture> getTextures() {
        return Collections.unmodifiableList(textureList);
    }

    @Override
    public TextureRegion getTexture(String name) {
        return textures.get(name.substring(0, name.lastIndexOf('.')));
    }
}
