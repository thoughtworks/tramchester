package com.tramchester.domain;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IdMap<T extends HasId> implements Iterable<T> {
    private final HashMap<String, T> theMap;

    public IdMap() {
        theMap = new HashMap<>();
    }

    public void add(T item) {
        theMap.put(item.getId(), item);
    }

    public void clear() {
        theMap.clear();
    }

    public boolean hasId(String id) {
        return theMap.containsKey(id);
    }

    public T get(String id) {
        return theMap.get(id);
    }

    public Set<String> getIds() {
        return new HashSet<>(theMap.keySet());
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

    public T getOrAdd(String id, Creates<T> constructor) {
        if (hasId(id)) {
            return get(id);
        }
        T value = constructor.create();
        add(value);
        return value;
    }

    public Set<T> filter(Filter<T> theFilter) {
        return theMap.values().stream().filter(theFilter::include).collect(Collectors.toUnmodifiableSet());
    }

    public interface Creates<T> {
        T create();
    }

    public interface Filter<T> {
        boolean include(T item);
    }
}
