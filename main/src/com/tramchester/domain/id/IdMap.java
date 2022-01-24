package com.tramchester.domain.id;

import com.google.common.collect.Sets;
import com.tramchester.domain.GraphProperty;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class IdMap<T extends HasId<T>> extends CompositeIdMap<T,T> {

    public IdMap(Set<T> items) {
        super(items);
    }

    public IdMap() {
        super();
    }

    private IdMap<T> addAll(IdMap<T> others) {
        super.addAll(others);
        return this;
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
                return items -> items;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        };
    }
}
