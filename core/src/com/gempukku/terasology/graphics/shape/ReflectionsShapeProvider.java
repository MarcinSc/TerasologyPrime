package com.gempukku.terasology.graphics.shape;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.graphics.environment.renderer.BlockSide;
import com.gempukku.terasology.prefab.PrefabComponentData;
import com.gempukku.terasology.prefab.PrefabData;
import com.gempukku.terasology.world.component.TexturePart;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RegisterSystem(
        shared = ShapeProvider.class)
public class ReflectionsShapeProvider implements ShapeProvider, LifeCycleSystem {

    private Map<String, ShapeDef> shapesById = new HashMap<>();

    @Override
    public void preInitialize() {
        Configuration scanPrefabs = new ConfigurationBuilder()
                .setScanners(new ShapeScanner())
                .setUrls(ClasspathHelper.forJavaClassPath());

        Reflections reflections = new Reflections(scanPrefabs);
        Multimap<String, String> resources = reflections.getStore().get(ShapeScanner.class);

        for (String shapeId : resources.keySet()) {
            Collection<String> paths = resources.get(shapeId);
            if (paths.size()>1)
                throw new IllegalStateException("More than one shape with the same name found: "+shapeId);

            try {
                InputStream shapeDefInputStream = ReflectionsShapeProvider.class.getResourceAsStream("/"+paths.iterator().next());
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ShapeDef shapeDef = objectMapper.readValue(shapeDefInputStream, ShapeDef.class);
                    shapesById.put(shapeId, shapeDef);
                } finally {
                    shapeDefInputStream.close();
                }
            } catch (IOException exp) {
                throw new RuntimeException("Unable to read shape data", exp);
            }
        }
    }

    @Override
    public ShapeDef getShapeById(String shapeId) {
        return shapesById.get(shapeId);
    }
//
//    public static void main(String[] args) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        ShapeDef source = mapper.readValue(new File("/Users/marcin.sciesinski/git/libgdx-test/core/assets/shape/cube.shape"), ShapeDef.class);
//
//        ShapeDef cube = new ShapeDef();
//        List<ShapePartDef> parts = new LinkedList<>();
//        for (ShapePartDef blockSide : source.getShapeParts()) {
//            ShapePartDef part = new ShapePartDef();
//
//            List<Float[]> vertices = new LinkedList<>();
//            List<Float[]> normals = new LinkedList<>();
//            List<Float[]> uvs = new LinkedList<>();
//            List<Short> indices = new LinkedList<>();
//
//            for (int i=0; i<blockSide.getVertices().size(); i+=6) {
//                vertices.add(blockSide.getVertices().get(i));
//                vertices.add(blockSide.getVertices().get(i+1));
//                vertices.add(blockSide.getVertices().get(i+2));
//                vertices.add(blockSide.getVertices().get(i+4));
//
//                normals.add(blockSide.getNormals().get(i));
//                normals.add(blockSide.getNormals().get(i+1));
//                normals.add(blockSide.getNormals().get(i+2));
//                normals.add(blockSide.getNormals().get(i+4));
//
//                uvs.add(blockSide.getUvs().get(i));
//                uvs.add(blockSide.getUvs().get(i+1));
//                uvs.add(blockSide.getUvs().get(i+2));
//                uvs.add(blockSide.getUvs().get(i+4));
//
//                indices.add((short) 0);
//                indices.add((short) 1);
//                indices.add((short) 2);
//                indices.add((short) 2);
//                indices.add((short) 3);
//                indices.add((short) 0);
//            }
//
//            part.setVertices(vertices);
//            part.setNormals(normals);
//            part.setUvs(uvs);
//            part.setIndices(indices);
//            part.setTextures(blockSide.getTextures());
//            part.setSide(blockSide.getSide());
//
//            parts.add(part);
//        }
//        cube.setShapeParts(parts);
//
//        cube.setFullParts(Arrays.asList("top", "bottom", "front", "back", "left", "right"));
//
//        mapper.writeValue(new File("/Users/marcin.sciesinski/git/libgdx-test/core/assets/shape/cube2.shape"), cube);
//    }
//
//    private static void addPosition(List<Float[]> array, Vector2 position) {
//        array.add(new Float[] {position.x, position.y});
//    }
//
//    private static void addVertex(List<Float[]> array, Vector3 vertex) {
//        array.add(new Float[] {vertex.x, vertex.y, vertex.z});
//    }

    private static class ShapeScanner extends ResourcesScanner {
        private String extension = ".shape";
        private int extensionLength = extension.length();

        public boolean acceptsInput(String file) {
            return file.endsWith(extension);
        }

        public Object scan(Vfs.File file, Object classObject) {
            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.length()-extensionLength);
            this.getStore().put(fileName, file.getRelativePath());
            return classObject;
        }
    }
}
