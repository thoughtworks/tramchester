package com.tramchester.domain.id;


import com.google.common.collect.Sets;
import com.tramchester.domain.CoreDomain;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdSet<T extends CoreDomain> implements Iterable<IdFor<T>> {
    private final Set<IdFor<T>> theSet;

    public IdSet() {
        theSet = new HashSet<>();
    }

    public IdSet(Set<IdFor<T>> set) {
        this(set, true);
    }

    public static <W extends CoreDomain> IdSet<W> wrap(Set<IdFor<W>> set) {
        return new IdSet<>(set, false);
    }

    private IdSet(Set<IdFor<T>> set, boolean copy) {
        theSet = copy ? new HashSet<>(set) : set;
    }

    public IdSet(Collection<IdFor<T>> ids) {
        this();
        theSet.addAll(ids);
    }

    public IdSet(IdFor<T> id) {
        this();
        theSet.add(id);
    }

    public IdSet(int initialCapabicity) {
        theSet = new HashSet<>(initialCapabicity);
    }

    public static <T extends CoreDomain> IdSet<T> singleton(IdFor<T> id) {
        IdSet<T> result = new IdSet<>();
        result.add(id);
        return result;
    }

    public static <T extends CoreDomain> IdSet<T> emptySet() {
        return new IdSet<>(Collections.emptySet());
    }

    // TODO Rework generics for this class
    public static <S extends CoreDomain & HasId<S>> IdSet<S> from(Set<S> items) {
        Set<IdFor<S>> ids = items.stream().map(HasId::getId).collect(Collectors.toSet());
        return wrap(ids);
    }

    public IdSet<T> addAll(IdSet<T> other) {
        theSet.addAll(other.theSet);
        return this;
    }

    public IdSet<T> add(IdFor<T> id) {
        theSet.add(id);
        return this;
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

    public static <T extends HasId<T> & CoreDomain> Collector<T, IdSet<T>, IdSet<T>> collector() {
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

    public static <T extends CoreDomain> Collector<IdFor<T>, IdSet<T>, IdSet<T>> idCollector() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdSet<?> idSet = (IdSet<?>) o;

        return theSet.equals(idSet.theSet);
    }

    @Override
    public int hashCode() {
        return theSet.hashCode();
    }

    public boolean containsAll(IdSet<T> other) {
        return theSet.containsAll(other.theSet);
    }

    /***
     * primarily for test support
     * @return a new list containing the idset
     */
    public List<IdFor<T>> toList() {
        return new ArrayList<>(theSet);
    }

}
