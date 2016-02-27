package com.gempukku.terasology.component;

import com.gempukku.secsy.context.annotation.RegisterSystem;
import com.gempukku.secsy.context.system.LifeCycleSystem;
import com.gempukku.secsy.entity.Component;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(
        shared = TerasologyComponentManager.class)
public class ReflectionsComponentManager implements TerasologyComponentManager, LifeCycleSystem {
    private Map<String, Class<? extends Component>> prefabsByName = new HashMap<>();

    @Override
    public void preInitialize() {
        Configuration scanPrefabs = new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(true))
                .setUrls(ClasspathHelper.forJavaClassPath());

        Reflections reflections = new Reflections(scanPrefabs);
        for (Class<? extends Component> component : reflections.getSubTypesOf(Component.class)) {
            prefabsByName.put(component.getSimpleName(), component);
        }
    }

    @Override
    public Class<? extends Component> getComponentByName(String name) {
        return prefabsByName.get(name);
    }
}
