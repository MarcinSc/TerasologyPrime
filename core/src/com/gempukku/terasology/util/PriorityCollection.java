package com.gempukku.terasology.util;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

public class PriorityCollection<T> implements Iterable<T> {
    private Multimap<Integer, T> multimap = Multimaps.newMultimap(new TreeMap<>(Collections.reverseOrder()),
            (Supplier<Collection<T>>) ArrayList::new);

    public void add(T t) {
        int priority = 0;
        if (t instanceof Prioritable)
            priority = ((Prioritable) t).getPriority();

        multimap.put(priority, t);
    }

    @Override
    public Iterator<T> iterator() {
        return multimap.values().iterator();
    }
}
