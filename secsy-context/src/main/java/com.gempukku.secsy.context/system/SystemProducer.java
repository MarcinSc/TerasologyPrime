package com.gempukku.secsy.context.system;

import java.util.Collection;

/**
 * Interface that produces systems that should participate in a context.
 * @param <S>
 */
public interface SystemProducer<S> {
    Collection<S> createSystems();
}
