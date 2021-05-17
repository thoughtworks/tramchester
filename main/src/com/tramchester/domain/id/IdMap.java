package com.tramchester.domain.id;

import com.google.common.collect.Sets;
import com.tramchester.domain.GraphProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class IdMap<T extends HasId<T> & GraphProperty> implements Iterable<T> {
    private final HashMap<IdFor<T>, T> theMap;

    public IdMap() {
        theMap = new HashMap<>();
    }

    public void add(T item) {
        theMap.put(item.getId(), item);
    }

    public void clear() {
        theMap.clear();
    }

    public boolean hasId(IdFor<T> id) {
        return theMap.containsKey(id);
    }

    public T get(IdFor<T> id) {
        return theMap.get(id);
    }

    public IdSet<T> getIds() {
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

    public T getOrAdd(IdFor<T> id, Creates<T> constructor) {
        if (hasId(id)) {
            return get(id);
        }
        T value = constructor.create();
        add(value);
        return value;
    }

    public Stream<T> filterStream(Filter<T> theFilter) {
        return theMap.values().stream().filter(theFilter::include);
    }

    private IdMap<T> addAll(IdMap<T> others) {
        theMap.putAll(others.theMap);
        return this;
    }

    public Stream<T> getValuesStream() {
        return theMap.values().stream();
    }

    public interface Creates<T> {
        T create();
    }

    public interface Filter<T> {
        boolean include(T item);
    }

    public static <T extends HasId<T> & GraphProperty> Collector<T, IdMap<T>, IdMap<T>> collector() {
        return new Collector<>() {
            @Override
            public Supplier<IdMap<T>> supplier() {
                return IdMap::new;
            }

            @Override
            public BiConsumer<IdMap<T>, T> accumulator() {
                return IdMap::add;
            }

            @Override
            public BinaryOperator<IdMap<T>> combiner() {
                return IdMap::addAll;
            }

            @Override
            public Function<IdMap<T>, IdMap<T>> finisher() {
                return stations -> stations;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        };
    }

    @Override
    public String toString() {
        return "IdMap{" + theMap + '}';
    }
}
