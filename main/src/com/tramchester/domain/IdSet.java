package com.tramchester.domain;


import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class IdSet<T> implements Iterable<IdFor<T>> {
    private final Set<IdFor<T>> theSet;

    public IdSet() {
        theSet = new HashSet<>();
    }

    public IdSet(Set<IdFor<T>> set) {
        theSet = new HashSet<>(set);
    }

    public static <T> IdSet<T> singleton(IdFor<T> id) {
        IdSet<T> result = new IdSet<>();
        result.add(id);
        return result;
    }

    private IdSet<T> addAll(IdSet<T> other) {
        theSet.addAll(other.theSet);
        return this;
    }

    public void add(IdFor<T> id) {
        theSet.add(id);
    }

    public int size() {
        return theSet.size();
    }

    public boolean contains(IdFor<T> id) {
        return theSet.contains(id);
    }

    public void clear() {
        theSet.clear();
    }

    public boolean isEmpty() {
        return theSet.isEmpty();
    }

    public void remove(IdFor<T> id) {
        theSet.remove(id);
    }

    public Stream<IdFor<T>> stream() {
        return theSet.stream();
    }

    public static <T extends HasId<T>> Collector<T, IdSet<T>, IdSet<T>> collector() {
        return new Collector<>() {
            @Override
            public Supplier<IdSet<T>> supplier() {
                return IdSet::new;
            }

            @Override
            public BiConsumer<IdSet<T>, T> accumulator() {
                return (theSet, item) -> theSet.add(item.getId());
            }

            @Override
            public BinaryOperator<IdSet<T>> combiner() {
                return IdSet::addAll;
            }

            @Override
            public Function<IdSet<T>, IdSet<T>> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        };
    }

    public static <T extends HasId<T>> Collector<IdFor<T>, IdSet<T>, IdSet<T>> idCollector() {
        return new Collector<>() {
            @Override
            public Supplier<IdSet<T>> supplier() {
                return IdSet::new;
            }

            @Override
            public BiConsumer<IdSet<T>, IdFor<T>> accumulator() {
                return IdSet::add;
            }

            @Override
            public BinaryOperator<IdSet<T>> combiner() {
                return IdSet::addAll;
            }

            @Override
            public Function<IdSet<T>, IdSet<T>> finisher() {
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        };
    }

    @NotNull
    @Override
    public Iterator<IdFor<T>> iterator() {
        return theSet.iterator();
    }

    @Override
    public void forEach(Consumer<? super IdFor<T>> action) {
        theSet.forEach(action);
    }

    @Override
    public String toString() {
        return "IdSet{" + theSet + '}';
    }
}
