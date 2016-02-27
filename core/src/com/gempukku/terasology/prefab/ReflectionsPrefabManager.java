package com.gempukku.terasology.prefab;

import com.gempukku.secsy.context.annotation.In;
import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.Component;
import com.gempukku.secsy.entity.io.ComponentData;
import com.gempukku.secsy.entity.io.EntityData;
import com.gempukku.terasology.component.TerasologyComponentManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RegisterSystem(
        shared = PrefabManager.class)
public class ReflectionsPrefabManager implements PrefabManager, LifeCycleSystem {
    @In
    private TerasologyComponentManager terasologyComponentManager;

    private Map<String, PrefabData> prefabsByName = new HashMap<>();

    @Override
    public void preInitialize() {
        Configuration scanPrefabs = new ConfigurationBuilder()
                .setScanners(new PrefabsScanner())
                .setUrls(ClasspathHelper.forJavaClassPath());

        Reflections reflections = new Reflections(scanPrefabs);
        Multimap<String, String> resources = reflections.getStore().get(PrefabsScanner.class);

        for (String prefabName : resources.keySet()) {
            Collection<String> paths = resources.get(prefabName);
            if (paths.size()>1)
                throw new IllegalStateException("More than one prefab with the same name found: "+prefabName);

            try {
                InputStream prefabInputStream = ReflectionsPrefabManager.class.getResourceAsStream("/"+paths.iterator().next());
                try {
                    PrefabData prefabData = readPrefabData(prefabInputStream);
                    prefabsByName.put(prefabName, prefabData);
                } finally {
                    prefabInputStream.close();
                }
            } catch (IOException | ParseException exp) {
                throw new RuntimeException("Unable to read prefab data", exp);
            }
        }
    }

    @Override
    public Iterable<PrefabData> findPrefabsWithComponents(Class<? extends Component>... components) {
        return Iterables.filter(
                prefabsByName.values(),
                new Predicate<PrefabData>() {
                    @Override
                    public boolean apply(PrefabData prefabData) {
                        Map<String, PrefabComponentData> prefabComponents = prefabData.getComponents();
                        for (Class<? extends Component> component : components) {
                            if (!prefabComponents.containsKey(terasologyComponentManager.getNameByComponent(component)))
                                return false;
                        }

                        return true;
                    }
                });
    }

    @Override
    public PrefabData getPrefabByName(String name) {
        return prefabsByName.get(name);
    }

    @Override
    public EntityData convertToEntityData(final PrefabData prefabData) {
        return new EntityData() {
            @Override
            public int getEntityId() {
                return 0;
            }

            @Override
            public Iterable<ComponentData> getComponents() {
                return prefabData.getComponents().entrySet().stream().map(componentData ->
                        new ComponentData() {
                            @Override
                            public Class<? extends Component> getComponentClass() {
                                Class<? extends Component> componentClass = terasologyComponentManager.getComponentByName(componentData.getKey());
                                if (componentClass == null)
                                    System.out.println("Unable to find component for entity: "+componentData.getKey());
                                return componentClass;
                            }

                            @Override
                            public Iterable<FieldNameAndValue> getFields() {
                                return componentData.getValue().getFields().entrySet().stream().map(
                                        fieldNameAndValue ->
                                                new FieldNameAndValue(fieldNameAndValue.getKey(), fieldNameAndValue.getValue())).collect(Collectors.toList());
                            }
                        }).collect(Collectors.toList());
            }
        };
    }

    private PrefabData readPrefabData(InputStream prefabInputStream) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject entity = (JSONObject) parser.parse(new InputStreamReader(prefabInputStream, Charset.forName("UTF-8")));

        Map<String, PrefabComponentData> componentData = new HashMap<>();
        for (String componentName : (Iterable<String>) entity.keySet()) {
            Map<String, Object> fieldValues = new HashMap<>();
            JSONObject componentObject = (JSONObject) entity.get(componentName);
            for(String fieldName : (Iterable<String>) componentObject.keySet()) {
                Object fieldValue = componentObject.get(fieldName);
                fieldValues.put(fieldName, fieldValue);
            }
            PrefabComponentData prefabComponentData = new PrefabComponentData(fieldValues);
            componentData.put(componentName, prefabComponentData);
        }
        return new PrefabData(componentData);
    }

    private static class PrefabsScanner extends ResourcesScanner {
        private String extension = ".prefab";
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
