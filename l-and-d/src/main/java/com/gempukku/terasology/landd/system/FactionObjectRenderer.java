package com.gempukku.terasology.landd.system;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.EntityRef;
import com.gempukku.secsy.entity.index.EntityIndex;
import com.gempukku.secsy.entity.index.EntityIndexManager;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.graphics.TextureAtlasRegistry;
import com.gempukku.terasology.graphics.environment.mesh.ArrayVertexOutput;
import com.gempukku.terasology.graphics.environment.renderer.EnvironmentRenderer;
import com.gempukku.terasology.graphics.environment.renderer.EnvironmentRendererRegistry;
import com.gempukku.terasology.graphics.shape.ShapeDef;
import com.gempukku.terasology.graphics.shape.ShapePartDef;
import com.gempukku.terasology.graphics.shape.ShapeProvider;
import com.gempukku.terasology.landd.component.RenderedObjectComponent;
import com.gempukku.terasology.world.component.LocationComponent;
import org.terasology.math.geom.Vector3f;

import java.util.Arrays;

@RegisterSystem(profiles = NetProfiles.CLIENT)
public class FactionObjectRenderer implements EnvironmentRenderer, LifeCycleSystem {
    @In
    private EnvironmentRendererRegistry environmentRendererRegistry;
    @In
    private ShapeProvider shapeProvider;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private TextureAtlasRegistry textureAtlasRegistry;
    @In
    private EntityIndexManager entityIndexManager;

    private Model model;
    private EntityIndex factionObjectIndex;

    @Override
    public void initialize() {
        environmentRendererRegistry.registerEnvironmentRendered(this);
        textureAtlasRegistry.registerTextures("terrain", Arrays.asList("blockTiles/bars/GoldBar.png"));
        factionObjectIndex = entityIndexManager.addIndexOnComponents(LocationComponent.class, RenderedObjectComponent.class);
    }

    @Override
    public void destroy() {
        if (model != null)
            model.dispose();
    }

    @Override
    public void renderEnvironment(Camera camera, String worldId, ModelBatch modelBatch) {
        initModel();

        for (EntityRef entityRef : factionObjectIndex.getEntities()) {
            LocationComponent location = entityRef.getComponent(LocationComponent.class);
            if (location.getWorldId().equals(worldId)) {
                ModelInstance modelInstance = new ModelInstance(model);
                modelInstance.transform.translate(location.getX(), location.getY(), location.getZ());
                modelBatch.render(modelInstance);
            }
        }
    }

    private void initModel() {
        if (model == null) {
            Vector3f tempVector = new Vector3f();

            FloatArray vertices = new FloatArray();
            ShortArray indices = new ShortArray();

            TextureRegion texture = textureAtlasProvider.getTexture("terrain", "blockTiles/bars/GoldBar.png");

            ArrayVertexOutput vertexOutput = new ArrayVertexOutput(vertices, indices);
            ShapeDef cube = shapeProvider.getShapeById("cube");
            for (ShapePartDef shapePart : cube.getShapeParts()) {
                int vertexCount = shapePart.getVertices().size();

                // This array will store indexes of vertices in the resulting Mesh
                short[] vertexMapping = new short[vertexCount];

                // Trunk
                for (int vertex = 0; vertex < vertexCount; vertex++) {
                    Float[] vertexCoords = shapePart.getVertices().get(vertex);
                    Float[] normalValues = shapePart.getNormals().get(vertex);
                    Float[] textureCoords = shapePart.getUvs().get(vertex);

                    tempVector.set(vertexCoords[0], vertexCoords[1], vertexCoords[2])
                            .add(-0.5f, 0f, -0.5f).mul(1, 2, 1);

                    vertexOutput.setPosition(tempVector.x, tempVector.y, tempVector.z);
                    vertexOutput.setNormal(normalValues[0], normalValues[1], normalValues[2]);
                    vertexOutput.setTextureCoordinate(
                            texture.getU() + textureCoords[0] * (texture.getU2() - texture.getU()),
                            texture.getV() + textureCoords[1] * (texture.getV2() - texture.getV()));

                    vertexMapping[vertex] = vertexOutput.finishVertex();
                }
                for (short index : shapePart.getIndices()) {
                    vertexOutput.addVertexIndex(vertexMapping[index]);
                }
            }

            VertexAttribute customVertexInformation = new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_flag");

            Mesh mesh = new Mesh(true, vertices.size / 9, indices.size,
                    VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0),
                    customVertexInformation);
            mesh.setIndices(indices.toArray());
            mesh.setVertices(vertices.toArray());

            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.begin();

            MeshPart meshPart = new MeshPart("object", mesh, 0, indices.size, GL20.GL_TRIANGLES);
            modelBuilder.part(meshPart, new Material(TextureAttribute.createDiffuse(texture.getTexture())));
            model = modelBuilder.end();
        }
    }
}
