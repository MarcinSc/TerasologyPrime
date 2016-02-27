package com.gempukku.secsy.context.system;

import com.gempukku.secsy.context.SystemContext;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Interface that takes care of initializing and destroying classes.
 * @param <S>
 */
public interface SystemInitializer<S> {
    Map<Class<?>, S> initializeSystems(Collection<S> systems);
    void destroySystems(Collection<S> systems);
}
