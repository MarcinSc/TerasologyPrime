package com.gempukku.secsy.entity;

import com.gempukku.secsy.entity.event.Event;

import java.util.Collection;
import java.util.Collections;

public class NullEntityRef implements EntityRef {
    public static final EntityRef singleton = new NullEntityRef();

    private NullEntityRef() {

    }

    @Override
    public <T extends Component> T createComponent(Class<T> clazz) {
        throw new UnsupportedOperationException("Null entity cannot have component added");
    }

    @Override
    public <T extends Component> T getComponent(Class<T> clazz) {
        return null;
    }

    @Override
    public void saveComponents(Component... component) {
        throw new UnsupportedOperationException("Null entity cannot have components saved");
    }

    @Override
    public <T extends Component> void removeComponents(Class<T>... clazz) {
        throw new UnsupportedOperationException("Null entity cannot have components removed");
    }

    @Override
    public Collection<Class<? extends Component>> listComponents() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasComponent(Class<? extends Component> component) {
        return false;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public void send(Event event) {
    }
}
