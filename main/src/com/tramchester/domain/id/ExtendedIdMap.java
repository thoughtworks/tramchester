package com.tramchester.domain.id;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class ExtendedIdMap<S extends HasId<S>, T extends HasId<S>> implements Iterable<T> {
    private final HashMap<IdFor<S>, T> theMap;

    public ExtendedIdMap() {
        theMap = new HashMap<>();
    }

    public void add(T item) {
        theMap.put(item.getId(), item);
    }

    public void clear() {
        theMap.clear();
    }

    public boolean hasId(IdFor<S> id) {
        return theMap.containsKey(id);
    }

    public T get(IdFor<S> id) {
        return theMap.get(id);
    }

    public IdSet<S> getIds() {
        return new IdSet<>(theMap.keySet());
    }

    public int size() {
        return theMap.size();
    }

    public Set<T> getValues() {
        return new HashSet<>(theMap.values());
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return theMap.values().iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        theMap.values().forEach(action);
    }

    public T getOrAdd(IdFor<S> id, Creates<T> constructor) {
        if (hasId(id)) {
            return get(id);
        }
        T value = constructor.create();
        add(value);
        return value;
    }

    public boolean isEmpty() {
        return theMap.isEmpty();
    }

    public interface Creates<T> {
        T create();
    }

    public interface Filter<T> {
        boolean include(T item);
    }

    @Override
    public String toString() {
        return "IdMap{" + theMap + '}';
    }
}
