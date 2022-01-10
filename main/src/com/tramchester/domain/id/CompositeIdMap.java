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
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO Should not need GraphProperty Here
public class CompositeIdMap<S extends HasId<S> & GraphProperty, T extends S> implements Iterable<T> {
    private final HashMap<IdFor<S>, T> theMap;

    public CompositeIdMap() {
        theMap = new HashMap<>();
    }

    public CompositeIdMap(Set<T> items) {
        theMap = new HashMap<>(items.stream().collect(Collectors.toMap(HasId::getId, item -> item)));
    }

    protected static <A extends HasId<A>,B extends A> CompositeIdMap<A,B> from(Set<B> items) {
        return items.stream().collect(collector());
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

    public Set<S> getSuperValues() {
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

    public Stream<S> filterStream(Filter<S> theFilter) {
        return getValuesStream().filter(theFilter::include);
    }

    protected CompositeIdMap<S,T> addAll(CompositeIdMap<S,T> others) {
        theMap.putAll(others.theMap);
        return this;
    }

    public Stream<S> getValuesStream() {
        return theMap.values().stream().map(item -> item);
    }

    public boolean isEmpty() {
        return theMap.isEmpty();
    }

    public void remove(IdSet<S> keysToRemove) {
        keysToRemove.forEach(theMap::remove);
    }

    public interface Creates<T> {
        T create();
    }

    public interface Filter<T> {
        boolean include(T item);
    }

    private static <S extends HasId<S> & GraphProperty, T extends S > Collector<T, CompositeIdMap<S,T>, CompositeIdMap<S,T>> collector() {
        return new Collector<>() {
            @Override
            public Supplier<CompositeIdMap<S,T>> supplier() {
                return CompositeIdMap::new;
            }

            @Override
            public BiConsumer<CompositeIdMap<S,T>, T> accumulator() {
                return CompositeIdMap::add;
            }

            @Override
            public BinaryOperator<CompositeIdMap<S,T>> combiner() {
                return CompositeIdMap::addAll;
            }

            @Override
            public Function<CompositeIdMap<S,T>, CompositeIdMap<S,T>> finisher() {
                return items -> items;
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
