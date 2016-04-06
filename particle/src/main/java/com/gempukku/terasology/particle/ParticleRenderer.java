package com.gempukku.terasology.particle;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.NetProfiles;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.terasology.graphics.PostEnvironmentRenderer;
import com.gempukku.terasology.graphics.PostEnvironmentRendererRegistry;
import com.gempukku.terasology.graphics.TextureAtlasProvider;
import com.gempukku.terasology.time.TimeManager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Particle renderer uses a Mesh with a limit of 4000 vertices (1000 particles).
 * It packs the information the following way:
 * a_position - position of the particle in 3d space
 * a_normal - used to store the following information: rotation, scale, cornerNo
 * a_texCoord0 - stores texture coordinates UV for that vertex
 */
@RegisterSystem(
        profiles = NetProfiles.CLIENT, shared = ParticleEmitter.class)
public class ParticleRenderer implements PostEnvironmentRenderer, ParticleEmitter, LifeCycleSystem {
    @In
    private PostEnvironmentRendererRegistry postEnvironmentRendererRegistry;
    @In
    private TextureAtlasProvider textureAtlasProvider;
    @In
    private TimeManager timeManager;

    private static final int MAX_PARTICLE_COUNT = 1000;
    private static final float GRAVITY = -9.81f;

    private ParticleShaderProvider particleShaderProvider;
    private ModelBatch modelBatch;

    private Mesh mesh;

    private Model model;
    private ModelInstance modelInstance;

    private LinkedList<Particle> particles = new LinkedList<>();

    // 4 vertices per particle, 8 floats per vertex
    private float[] vertices = new float[4 * MAX_PARTICLE_COUNT * 8];
    private int lastUsedParticleCount = 0;

    @Override
    public void preInitialize() {
        particleShaderProvider = new ParticleShaderProvider();

        modelBatch = new ModelBatch(particleShaderProvider);
    }

    @Override
    public void initialize() {
        postEnvironmentRendererRegistry.registerPostEnvironmentRenderer(this);
    }

    @Override
    public void destroy() {
        if (model != null)
            model.dispose();
    }

    @Override
    public void emitParticle(Particle particle) {
        particles.add(particle);
    }

    @Override
    public void renderPostEnvironment(Camera camera, String worldId) {
        initModel();
        updateParticles();
        updateModel(worldId);

        modelBatch.begin(camera);
        modelBatch.render(modelInstance);
        modelBatch.end();
    }

    private void initModel() {
        if (model == null) {
            // Each particle takes 4 vertices (corners) and 6 indices (two triangles)
            mesh = new Mesh(false, true, 4 * MAX_PARTICLE_COUNT, 6 * MAX_PARTICLE_COUNT, new VertexAttributes(
                    VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0)));

            short[] indices = new short[6 * MAX_PARTICLE_COUNT];
            for (int i = 0; i < MAX_PARTICLE_COUNT; i++) {
                indices[6 * i] = (short) (i * 4);
                indices[6 * i + 1] = (short) (i * 4 + 1);
                indices[6 * i + 2] = (short) (i * 4 + 2);
                indices[6 * i + 3] = (short) (i * 4 + 2);
                indices[6 * i + 4] = (short) (i * 4 + 3);
                indices[6 * i + 5] = (short) (i * 4);
            }
            mesh.setIndices(indices);
            mesh.setVertices(vertices);

            List<Texture> particles = textureAtlasProvider.getTextures("particles");
            if (particles.size() > 1)
                throw new RuntimeException("At the moment particles have to be on one underlying texture");

            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.begin();

            MeshPart meshPart = new MeshPart("particles", mesh, 0, 6 * MAX_PARTICLE_COUNT, GL20.GL_TRIANGLES);
            modelBuilder.part(meshPart, new Material(TextureAttribute.createDiffuse(particles.get(0)),
                    new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE), new DepthTestAttribute(false)));
            model = modelBuilder.end();
            modelInstance = new ModelInstance(model);
        }
    }

    private void updateParticles() {
        float timeSinceLastUpdateInSeconds = timeManager.getTimeSinceLastUpdate() / 1000f;

        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            if (!particle.updateParticle(GRAVITY, timeSinceLastUpdateInSeconds))
                iterator.remove();
        }
    }

    private void updateModel(String worldId) {
        int particleCount = 0;
        Iterator<Particle> particleIterator = particles.descendingIterator();
        for (int i = 0; i < MAX_PARTICLE_COUNT; i++) {
            Particle particle = null;
            while (particleIterator.hasNext()) {
                Particle untestedParticle = particleIterator.next();
                if (untestedParticle.getWorldId().equals(worldId)) {
                    particle = untestedParticle;
                    break;
                }
            }

            if (particle != null) {
                TextureRegion textureRegion = particle.getTexture();
                Vector3 location = particle.getLocation();
                for (int corner = 0; corner < 4; corner++) {
                    vertices[i * 8 * 4 + corner * 8] = location.x;
                    vertices[i * 8 * 4 + corner * 8 + 1] = location.y;
                    vertices[i * 8 * 4 + corner * 8 + 2] = location.z;
                    vertices[i * 8 * 4 + corner * 8 + 3] = particle.getRotation();
                    vertices[i * 8 * 4 + corner * 8 + 4] = particle.getScale();
                    vertices[i * 8 * 4 + corner * 8 + 5] = corner + 1;
                    if (corner == 0) {
                        vertices[i * 8 * 4 + corner * 8 + 6] = textureRegion.getU();
                        vertices[i * 8 * 4 + corner * 8 + 7] = textureRegion.getV2();
                    } else if (corner == 1) {
                        vertices[i * 8 * 4 + corner * 8 + 6] = textureRegion.getU2();
                        vertices[i * 8 * 4 + corner * 8 + 7] = textureRegion.getV2();
                    } else if (corner == 2) {
                        vertices[i * 8 * 4 + corner * 8 + 6] = textureRegion.getU2();
                        vertices[i * 8 * 4 + corner * 8 + 7] = textureRegion.getV();
                    } else {
                        vertices[i * 8 * 4 + corner * 8 + 6] = textureRegion.getU();
                        vertices[i * 8 * 4 + corner * 8 + 7] = textureRegion.getV();
                    }
                }
                particleCount++;
            } else if (i < lastUsedParticleCount) {
                // This is to write over any old particles in the array
                for (int corner = 0; corner < 4; corner++) {
                    vertices[i * 8 * 4 + corner * 8] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 1] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 2] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 3] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 4] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 5] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 6] = 0;
                    vertices[i * 8 * 4 + corner * 8 + 7] = 0;
                }
            }
        }

        lastUsedParticleCount = particleCount;
        mesh.updateVertices(0, vertices, 0, 4 * particleCount * 8);
    }
}
