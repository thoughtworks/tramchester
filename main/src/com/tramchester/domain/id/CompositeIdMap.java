package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CompositeIdMap<S extends HasId<S> & GraphProperty, T extends S> implements Iterable<T> {
    private final HashMap<IdFor<S>, T> theMap;

    public CompositeIdMap() {
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

    public Stream<T> filterStream(Filter<T> theFilter) {
        return theMap.values().stream().filter(theFilter::include);
    }

    protected CompositeIdMap<S,T> addAll(CompositeIdMap<S,T> others) {
        theMap.putAll(others.theMap);
        return this;
    }

    public Stream<T> getValuesStream() {
        return theMap.values().stream();
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

//    public static <S extends HasId<S>,
//            T extends HasId<S> & GraphProperty> Collector<T, CompostiteIdMap<S,T>, CompostiteIdMap<S,T>> collector() {
//        return new Collector<>() {
//            @Override
//            public Supplier<CompostiteIdMap<S,T>> supplier() {
//                return CompostiteIdMap::new;
//            }
//
//            @Override
//            public BiConsumer<CompostiteIdMap<S,T>, T> accumulator() {
//                return CompostiteIdMap::add;
//            }
//
//            @Override
//            public BinaryOperator<CompostiteIdMap<S,T>> combiner() {
//                return CompostiteIdMap::addAll;
//            }
//
//            @Override
//            public Function<CompostiteIdMap<S,T>, CompostiteIdMap<S,T>> finisher() {
//                return items -> items;
//            }
//
//            @Override
//            public Set<Characteristics> characteristics() {
//                return Sets.immutableEnumSet(Characteristics.UNORDERED);
//            }
//        };
//    }

    @Override
    public String toString() {
        return "IdMap{" + theMap + '}';
    }
}
